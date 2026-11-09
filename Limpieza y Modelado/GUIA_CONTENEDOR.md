# Guía de Contenedores para el Pipeline de Análisis Deportivo

## Estructura del Proyecto

```
Limpieza y Modelado/
├── Dockerfile           # Construcción de la imagen
├── docker-compose.yml   # Orquestación del contenedor
├── requirements.txt     # Dependencias Python
├── work/               # Carpeta de trabajo (montada como volumen)
│   └── .gitkeep
├── Data/               # Datos originales (montado como volumen)
│   ├── IRSCAT.gpkg
│   ├── incidente-reportado-bogota-d-c.csv
│   └── Violencia_interpersonal_Colombia.csv
└── GUIA_CONTENEDOR.md  # Esta guía
```

---

## Tabla Comparativa: Comandos Docker vs Podman

| Operación | Docker | Podman |
|-----------|--------|--------|
| **a) Construir (Build)** | `docker-compose build` | `podman-compose build` |
| **b) Iniciar (Up / Run)** | `docker-compose up -d` | `podman-compose up -d` |
| **c) Detener y eliminar (Down)** | `docker-compose down` | `podman-compose down` |

---

## Instrucciones Detalladas

### 1. Requisitos Previos

- **Docker Engine** (versión 20.10+) **O** **Podman** (versión 3.0+)
- **docker-compose** (para Docker) **O** **podman-compose** (para Podman)
- Espacio en disco: ~3 GB libres

> **Nota para Windows**: Asegúrate de tener instalado Docker Desktop o Podman con soporte para WSL2.

---

### 2. Construcción (Build)

**Ubicación**: Escribe los comandos desde la carpeta `Limpieza y Modelado/`.

#### Con Docker
```bash
cd "c:\Users\carlo\Desktop\Javeriana\SEMESTRE IX\AM\Proyecto_Aprendizaje_De_Maquina\Limpieza y Modelado"
docker-compose build
```

#### Con Podman
```bash
cd "c:\Users\carlo\Desktop\Javeriana\SEMESTRE IX\AM\Proyecto_Aprendizaje_De_Maquina\Limpieza y Modelado"
podman-compose build
```

**Espera**: La primera construcción puede tomar 5-10 minutos (descarga de imágenes base + instalación de dependencias).

> **Nota importante para Podman**: Si encuentras errores relacionados con `jovyan` o permisos, asegúrate de que el Dockerfile use UID/GID numéricos (`1000:100`) en lugar de nombres de usuario. Esto es una limitación conocida de Podman con formato OCI.

---

### 3. Iniciar Servicio (Up / Run)

#### Con Docker
```bash
docker-compose up -d
```

#### Con Podman
```bash
podman-compose up -d
```

**Resultado esperado**:
```
[+] Running 2/2
 ✔ Container notebook  Started        2.1s
```

---

### 4. Verificar que Jupyter Lab está funcionando

#### Opción A: Ver logs en tiempo real
```bash
# Docker
docker-compose logs -f notebook

# Podman
podman-compose logs -f notebook
```

O directamente con `podman`:
```bash
podman logs limpiezaymodelado_notebook_1
```

Deberías ver una línea similar a:
```
[I 2026-11-09 19:41:56.739 ServerApp] Jupyter Server 2.21.1 is running at:
[I 2026-11-09 19:41:56.739 ServerApp] http://localhost:8888/lab
[I 2026-11-09 19:41:56.739 ServerApp]     http://127.0.0.1:8888/lab
```

#### Opción B: Verificar con curl (Windows PowerShell)
```powershell
Invoke-WebRequest -Uri "http://127.0.0.1:8888" -UseBasicParsing | Select-Object -ExpandProperty StatusCode
```

Si todo está bien, recibirás una respuesta HTTP 200.

#### Opción C: Verificar en el navegador

1. Abre tu navegador (Chrome, Firefox, Edge).
2. Navega a: `http://127.0.0.1:8888` o `http://127.0.0.1:8888/lab`
3. Deberías ver la interfaz de Jupyter Lab sin solicitar token o contraseña.

> **Nota**: El entorno está configurado sin token de seguridad (`token=''`) para uso local y offline.

---

### 5. Detener y Eliminar (Down)

#### Con Docker
```bash
docker-compose down
```

#### Con Podman
```bash
podman-compose down
```

**Resultado esperado**:
```
[+] Running 2/2
 ✔ Container notebook  Removed        1.8s
 ✔ Network ..._default  Removed       0.5s
```

---

## Comandos Adicionales Útiles

| Acción | Docker | Podman |
|--------|--------|--------|
| Ver contenedores activos | `docker-compose ps` | `podman-compose ps` |
| Reiniciar contenedor | `docker-compose restart` | `podman-compose restart` |
| Acceder a la shell del contenedor | `docker-compose exec notebook bash` | `podman-compose exec notebook bash` |
| Eliminar imágenes construidas | `docker-compose down --rmi all` | `podman-compose down --rmi all` |
| Ver logs de un contenedor específico | `docker-compose logs notebook` | `podman logs <container_id>` |

---

## Solución de Problemas Comunes

| Problema | Solución |
|----------|----------|
| **Error: port already allocated** | El puerto 8888 está en uso. Cierra otros Jupyter Lab o cambia el puerto en `docker-compose.yml` |
| **Error: permission denied** | En Linux/Mac, ejecuta con `sudo`. En Windows, verifica que Docker Desktop esté corriendo |
| **Error looking up group "jovyan"** | Este es un error conocido con Podman. El Dockerfile ya usa UID/GID numéricos (`1000:100`). Si el error persiste, limpia el caché con `podman-compose build --no-cache` |
| **El contenedor se reinicia constantemente** | Revisa los logs con `docker-compose logs notebook` para identificar el error |
| **No se accede a los archivos en `./work`** | Verifica que la carpeta `work/` exista y que el `docker-compose.yml` esté en la carpeta correcta |

---

## Solución de Problemas Específicos para Windows + Podman

Si usas **Podman en Windows**, sigue estos pasos adicionales:

1. **Configurar Podman como servicio** (una sola vez):
   ```powershell
   podman machine init
   podman machine start
   ```

2. **Verificar la máquina**:
   ```powershell
   podman machine info
   ```

3. **Usar `podman-compose` en lugar de `docker-compose`** (los comandos se muestran en la tabla anterior).

---

## Archivos y Volumenes

Los siguientes directorios se montan automáticamente:

- `./work` → `/home/jovyan/work` (código y notebooks generados)
- `./Data` → `/home/jovyan/work/Data` (datos originales: IRSCAT.gpkg, CSVs)

Esto significa que puedes editar archivos localmente y ver los cambios en tiempo real dentro del contenedor.

---

## Notas de Seguridad

⚠️ **Este entorno está optimizado para uso local y offline**. No expongas el puerto 8888 en redes públicas ni en servidores externos. La configuración deshabilita la autenticación para conveniencia, lo cual es inseguro para entornos compartidos.

Para producción, modifica el `docker-compose.yml` para incluir:
- Token o contraseña en `environment`
- Network isolation
- Resource limits (memoria, CPU)

---

## Resumen del Dockerfile Optimizado para Podman

El Dockerfile utiliza UID/GID numéricos (`1000:100`) en lugar de nombres de usuario para evitar problemas con `podman` y el formato OCI:

```dockerfile
# Copiar requirements.txt con permisos correctos usando UID/GID numéricos
COPY --chown=1000:100 requirements.txt /home/jovyan/work/

# Cambiar al usuario jovyan usando UID numérico
USER 1000
```

Esto garantiza compatibilidad total con Docker y Podman en Windows.