FROM quay.io/jupyter/scipy-notebook

USER root

# Instalar git-lfs
RUN apt-get update && apt-get install -y --no-install-recommends \
    git-lfs \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Configurar git-lfs
RUN git lfs install

# Copiar requirements.txt con permisos correctos usando UID/GID numéricos
# jovyan: uid=1000, gid=100
COPY --chown=1000:100 requirements.txt /home/jovyan/work/

# Cambiar al usuario jovyan para instalar paquetes Python
USER 1000

# Instalar dependencias
RUN pip install --no-cache-dir -r /home/jovyan/work/requirements.txt

# Establecer el directorio de trabajo
WORKDIR /home/jovyan/work

# Exponer el puerto
EXPOSE 8888