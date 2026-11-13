$ErrorActionPreference = 'Stop'

# Runtime secrets are supplied when the container starts, never baked into the
# image.
docker build -t projects-manager $PSScriptRoot
if ($LASTEXITCODE -ne 0) {
    throw "Docker image build failed with exit code $LASTEXITCODE"
}

$envFile = Join-Path $PSScriptRoot '.env.docker'
if (Test-Path $envFile) {
    Write-Host "Image built. Start it with the ignored local environment file:"
    Write-Host "  docker run -d --name projects-manager --env-file .env.docker -p 2026:2026 projects-manager"
} else {
    Write-Host "Image built. Set JWT_SECRET and PSM_ADMIN_PASSWORD at runtime before starting the container."
    Write-Host "For local Docker use, copy .env.example to .env.docker, set both values, then run:"
    Write-Host "  docker run -d --name projects-manager --env-file .env.docker -p 2026:2026 projects-manager"
}
