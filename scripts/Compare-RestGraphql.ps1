# Compara chamadas reais equivalentes, sem resultados previamente preenchidos.
# Requer API iniciada e um prestador que tenha serviços ativos.
# Exemplo: .\scripts\Compare-RestGraphql.ps1 -ProviderId 1 -Iterations 30
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, [long]::MaxValue)]
    [long]$ProviderId,
    [ValidateRange(5, 1000)]
    [int]$Iterations = 30,
    [ValidateRange(0, 20)]
    [int]$Warmup = 3,
    [ValidateRange(1, 100)]
    [int]$PageSize = 20,
    [string]$OutputDirectory = ''
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $benchmarkScriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
    $OutputDirectory = Join-Path $benchmarkScriptDirectory '..\performance-results'
}
Add-Type -AssemblyName System.Net.Http
$benchmarkBaseUrl = $BaseUrl.TrimEnd('/')
$benchmarkRestUrl = "$benchmarkBaseUrl/api/providers/$ProviderId/services?page=0&size=$PageSize&sort=id,asc"
$benchmarkGraphqlUrl = "$benchmarkBaseUrl/graphql"
$benchmarkQuery = 'query Compare($providerId: ID!, $first: Int!) { servicesByProvider(providerId: $providerId, first: $first) { id title description price category active createdAt updatedAt provider { id fullName bio avatarUrl } } }'
$benchmarkPayload = @{
    query = $benchmarkQuery
    variables = @{ providerId = "$ProviderId"; first = $PageSize }
} | ConvertTo-Json -Depth 6 -Compress
$benchmarkPayloadBytes = [Text.Encoding]::UTF8.GetByteCount($benchmarkPayload)
$benchmarkClient = New-Object System.Net.Http.HttpClient
$benchmarkClient.Timeout = [TimeSpan]::FromSeconds(30)
$benchmarkClient.DefaultRequestHeaders.Accept.ParseAdd('application/json')

function Invoke-BenchmarkCall([string]$Protocol) {
    $benchmarkRequest = $null
    $benchmarkResponse = $null
    try {
        if ($Protocol -eq 'REST') {
            $benchmarkRequest = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, $benchmarkRestUrl)
            $benchmarkRequestBytes = 0
        } else {
            $benchmarkRequest = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, $benchmarkGraphqlUrl)
            $benchmarkRequest.Content = [System.Net.Http.StringContent]::new($benchmarkPayload, [Text.Encoding]::UTF8, 'application/json')
            $benchmarkRequestBytes = $benchmarkPayloadBytes
        }
        $benchmarkClock = [Diagnostics.Stopwatch]::StartNew()
        $benchmarkResponse = $benchmarkClient.SendAsync($benchmarkRequest).GetAwaiter().GetResult()
        $benchmarkBytes = $benchmarkResponse.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
        $benchmarkClock.Stop()
        $benchmarkBody = [Text.Encoding]::UTF8.GetString($benchmarkBytes)
        if (-not $benchmarkResponse.IsSuccessStatusCode) {
            throw "$Protocol retornou HTTP $([int]$benchmarkResponse.StatusCode): $benchmarkBody"
        }
        # A interpretação do JSON fica fora do cronômetro.
        $benchmarkJson = $benchmarkBody | ConvertFrom-Json
        if ($Protocol -eq 'GraphQL') {
            if ($benchmarkJson.errors) { throw "GraphQL retornou errors: $benchmarkBody" }
            $benchmarkItems = @($benchmarkJson.data.servicesByProvider)
        } else {
            $benchmarkItems = @($benchmarkJson.content)
        }
        $benchmarkNormalized = @($benchmarkItems | Sort-Object { [long]$_.id } | ForEach-Object {
            [ordered]@{
                id = "$($_.id)"; title = $_.title; description = $_.description
                price = ([decimal]$_.price).ToString([Globalization.CultureInfo]::InvariantCulture)
                category = $_.category; active = $_.active
                providerId = "$($_.provider.id)"; providerName = $_.provider.fullName
                bio = $_.provider.bio; avatarUrl = $_.provider.avatarUrl
            }
        }) | ConvertTo-Json -Depth 5 -Compress
        return [pscustomobject]@{
            protocol = $Protocol; elapsedMs = [math]::Round($benchmarkClock.Elapsed.TotalMilliseconds, 3)
            requestBodyBytes = $benchmarkRequestBytes; responseBodyBytes = $benchmarkBytes.Length
            httpStatus = [int]$benchmarkResponse.StatusCode; itemCount = $benchmarkItems.Count
            normalizedData = $benchmarkNormalized
        }
    } finally {
        if ($benchmarkResponse) { $benchmarkResponse.Dispose() }
        if ($benchmarkRequest) { $benchmarkRequest.Dispose() }
    }
}

function Get-Percentile([double[]]$Values, [double]$Percentile) {
    $benchmarkSorted = @($Values | Sort-Object)
    $benchmarkIndex = [math]::Max(0, [math]::Ceiling($Percentile * $benchmarkSorted.Count) - 1)
    return $benchmarkSorted[$benchmarkIndex]
}

try {
    $benchmarkPreflightRest = Invoke-BenchmarkCall 'REST'
    $benchmarkPreflightGraphql = Invoke-BenchmarkCall 'GraphQL'
    if ($benchmarkPreflightRest.itemCount -eq 0) {
        throw 'O prestador não tem serviços ativos. Cadastre ao menos um serviço para uma comparação útil.'
    }
    if ($benchmarkPreflightRest.normalizedData -ne $benchmarkPreflightGraphql.normalizedData) {
        throw 'REST e GraphQL retornaram conjuntos diferentes; verifique a ordenação e mantenha os dados estáveis.'
    }
    $benchmarkExpected = $benchmarkPreflightRest.normalizedData
    for ($benchmarkWarmupIndex = 0; $benchmarkWarmupIndex -lt $Warmup; $benchmarkWarmupIndex++) {
        Invoke-BenchmarkCall 'REST' | Out-Null
        Invoke-BenchmarkCall 'GraphQL' | Out-Null
    }
    $benchmarkSamples = New-Object 'System.Collections.Generic.List[object]'
    for ($benchmarkIteration = 1; $benchmarkIteration -le $Iterations; $benchmarkIteration++) {
        $benchmarkOrder = if ($benchmarkIteration % 2 -eq 1) { @('REST', 'GraphQL') } else { @('GraphQL', 'REST') }
        foreach ($benchmarkProtocol in $benchmarkOrder) {
            $benchmarkSample = Invoke-BenchmarkCall $benchmarkProtocol
            if ($benchmarkSample.normalizedData -ne $benchmarkExpected) {
                throw 'Os dados mudaram durante a medição. Repita sem cadastrar/alterar/remover serviços.'
            }
            $benchmarkSamples.Add([pscustomobject]@{
                iteration = $benchmarkIteration; protocol = $benchmarkSample.protocol
                elapsedMs = $benchmarkSample.elapsedMs; httpStatus = $benchmarkSample.httpStatus
                itemCount = $benchmarkSample.itemCount; requestBodyBytes = $benchmarkSample.requestBodyBytes
                responseBodyBytes = $benchmarkSample.responseBodyBytes
            })
        }
    }
    $benchmarkSummary = @($benchmarkSamples | Group-Object protocol | ForEach-Object {
        $benchmarkGroup = $_.Group
        $benchmarkTimes = [double[]]@($benchmarkGroup.elapsedMs)
        [pscustomobject]@{
            protocol = $_.Name; samples = $benchmarkGroup.Count
            meanMs = [math]::Round(($benchmarkTimes | Measure-Object -Average).Average, 3)
            p50Ms = Get-Percentile $benchmarkTimes 0.50; p95Ms = Get-Percentile $benchmarkTimes 0.95
            minMs = ($benchmarkTimes | Measure-Object -Minimum).Minimum
            maxMs = ($benchmarkTimes | Measure-Object -Maximum).Maximum
            meanRequestBodyBytes = [math]::Round(($benchmarkGroup.requestBodyBytes | Measure-Object -Average).Average, 1)
            meanResponseBodyBytes = [math]::Round(($benchmarkGroup.responseBodyBytes | Measure-Object -Average).Average, 1)
        }
    })
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
    $benchmarkRunId = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff')
    $benchmarkCsvPath = Join-Path $OutputDirectory "$benchmarkRunId-samples.csv"
    $benchmarkJsonPath = Join-Path $OutputDirectory "$benchmarkRunId-summary.json"
    $benchmarkSamples | Export-Csv -LiteralPath $benchmarkCsvPath -NoTypeInformation -Encoding UTF8
    [ordered]@{
        measuredAtUtc = [DateTime]::UtcNow.ToString('o'); baseUrl = $benchmarkBaseUrl
        providerId = $ProviderId; pageSize = $PageSize; warmupPerProtocol = $Warmup
        iterationsPerProtocol = $Iterations; itemCount = $benchmarkPreflightRest.itemCount
        methodology = 'Processo sequencial com conexões reutilizadas, aquecimento e ordem alternada. Cronômetro inclui envio e leitura do corpo, não parse JSON. Bytes representam somente corpos HTTP, sem cabeçalhos. REST inclui metadados de paginação e GraphQL inclui envelope data. Mesmo conjunto e campos do serviço; não mede throughput concorrente. Dados devem permanecer estáveis.'
        summary = $benchmarkSummary
    } | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $benchmarkJsonPath -Encoding UTF8
    $benchmarkSummary | Format-Table -AutoSize
    Write-Output "Amostras reais: $benchmarkCsvPath"
    Write-Output "Metodologia e resumo: $benchmarkJsonPath"
} finally {
    $benchmarkClient.Dispose()
}
