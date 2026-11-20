# Docker deployment

The root `Dockerfile` builds the Angular frontend and Spring Boot backend into
one image. Spring serves the frontend and API from the same origin. The image
includes the SQLite JDBC driver; Spring creates the SQLite schema on first
startup. The database file is kept in a Docker volume so it survives container
recreation.
 
## Render

The Render Blueprint in `render.yaml` configures the Docker service and asks
Render to generate `JWT_SECRET` and `PSM_ADMIN_PASSWORD` as runtime secrets.
Deploy through Render Blueprints to apply it. For an existing service that is
not managed by a Blueprint, add these two variables in the service's
Environment settings and redeploy. Do not put either value in the Dockerfile,
image, or source control. The admin password is available in the Render
environment settings after deployment.

Render provides `PORT` at runtime; Spring binds to that port automatically.

Build the image:

```powershell
.\build-image.ps1
```

For local Docker runs, create an ignored `.env.docker` from `.env.example` and
set non-placeholder values for `JWT_SECRET` and `PSM_ADMIN_PASSWORD`. These are
runtime settings; the image does not contain application secrets.

Run the image locally with its runtime settings:

```powershell
docker run -d --name projects-manager --env-file .env.docker -p 2026:2026 projects-manager
```

Docker automatically creates storage for `/var/lib/psm`; SQLite creates the
schema and database on first startup. Uploaded files are stored under
`/var/lib/psm/uploads`, inside that same automatically created volume, so there
is still no extra setup. The database and files persist across stops and starts
of that container, but removing the container and creating a new one does not
automatically reuse its anonymous volume. Reuse a named volume if the database
and uploads must survive container removal.

**Security:** keep runtime secrets in Render's environment settings or the
ignored local `.env.docker` file. Never commit the local environment file or
build secrets into the image.
