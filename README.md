<div align="center">

# 🛡️ BogotáSafe — Motor de Inferencia Offline de Riesgo Urbano Proyecto_Aprendizaje_De_Máquina

**Analítica predictiva y prevención ciudadana para Bogotá D.C. bajo arquitectura offline-first.**

[![Python](https://img.shields.io/badge/Python-3.10%2B-blue?logo=python&logoColor=white)](https://www.python.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-purple?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green?logo=android&logoColor=white)](https://www.android.com/)
[![Containers](https://img.shields.io/badge/Containers-Podman%20%7C%20Docker-orange?logo=podman&logoColor=white)](https://podman.io/)

</div>

---

## 📑 Tabla de Contenidos

- [Descripción General](#-descripción-general)
- [Fuentes de Datos Oficiales](#-fuentes-de-datos-oficiales)
- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Modelos y Fundamentación Matemática](#-modelos-y-fundamentación-matemática)
  - [1. Dimensión Espacial ($K$-Means, $k=7$)](#1-dimensión-espacial-k-means-k7)
  - [2. Dimensión Temporal (GLM de Poisson)](#2-dimensión-temporal-glm-de-poisson)
  - [3. Dimensión Demográfica (Naive Bayes)](#3-dimensión-demográfica-naive-bayes)
  - [4. Ensamble del Score Final](#4-ensamble-del-score-final)
- [Modos de la Aplicación Móvil](#-modos-de-la-aplicación-móvil)
- [Ejecución y Despliegue](#-ejecución-y-despliegue)
  - [Fase 1: Pipeline de Datos (Podman / Docker)](#fase-1-pipeline-de-datos-podman--docker)
  - [Fase 2: Aplicación Móvil (Instalación por APK)](#fase-2-aplicación-móvil-instalación-por-apk)

---

## 📖 Descripción General

**BogotáSafe** es una solución de ingeniería orientada a la seguridad ciudadana que traslada modelos de machine learning entrenados sobre microdatos delictivos distritales a un entorno móvil totalmente desconectado. 

A diferencia de los enfoques convencionales que consultan APIs en la nube, el sistema destila el conocimiento de más de 1,100 sectores catastrales urbanos, patrones temporales multianuales y probabilidades demográficas en un único artefacto ligero (`modelo_seguridad_bogota.json`, ≈ 150 KB). permitiendo calcular probabilidades e índices de riesgo en tiempo real directamente en un dispositivo móvil Android sin requerir conexión a internet ni servidores externos.

---

## 📊 Fuentes de Datos Oficiales

El proyecto se sustenta exclusivamente en registros abiertos de entidades oficiales colombianas:

| Dimensión / Componente | Conjunto de Datos | Propósito en el Modelo | Enlace Oficial |
| :--- | :--- | :--- | :--- |
| **Temporal** <br>*(Estacionalidad, Día y Hora)* | Estadísticas de Delitos Georreferenciados | Extracción de series históricas de eventos para calibrar la regresión de Poisson según mes, día de la semana y curva circadiana. | [Queremos Datos / SIEDCO](https://www.queremosdatos.co/request/estadisticas_de_delitos_georrefe_3?utm_) |
| **Ubicación** <br>*(Espacial / Catastral)* | Incidentes Reportados Bogotá D.C. | Georreferenciación de hechos en vía pública y cruce con polígonos catastrales urbanos (IDECA) para tipificar sectores. | [Datos Abiertos Bogotá](https://datosabiertos.bogota.gov.co/dataset/incidente-reportado-bogota-d-c) |
| **Condiciones** <br>*(Demografía / Exposición)* | Violencia Interpersonal en Colombia (2015–2024) | Estimación de razones de verosimilitud bayesiana según sexo biológico y ciclo vital en hechos ocurridos en espacio público. | [Datos Abiertos Colombia (INMLCF)](https://www.datos.gov.co/Justicia-y-Derecho/Violencia-interpersonal-Colombia-a-os-2015-a-2024-/e3xi-4zq5/about_data) |



---

## 🔬 Modelos y Fundamentación Matemática

### 1. Dimensión Espacial ($K$-Means, $k=7$)
* **Criterio de agrupación:** Agrupamiento basado en **perfil y densidad delictiva** por $\text{km}^2$ (hurtos, lesiones y homicidios de la ventana 2023–2025) sobre la capa catastral urbana reproyectada (EPSG:3116). No agrupa por simple contigüidad geográfica, identificando tipologías delictivas análogas a lo largo de la ciudad.
* **Selección de $k=7$:**
  * **Davies-Bouldin Index:** $1.0024$ (mínima dispersión intra-clúster).
  * Validado mediante el método del codo (*Elbow Method*) y puntuaciones de Silhouette y Calinski-Harabasz.
* **Puntaje base:** Asignación escalonada según severidad media de delitos por clúster (valores base entre $15.0$ y $92.0$).

### 2. Dimensión Temporal (GLM de Poisson)
* **Formulación:** Modela el conteo de incidentes en vía pública mediante una Regresión Lineal Generalizada (GLM) con distribución de Poisson y enlace logarítmico:

$$
\log(\lambda) = \beta_0 + \sum \beta_{\text{mes}} X_{\text{mes}} + \sum \beta_{\text{dia}} X_{\text{dia}} + \sum \beta_{\text{hora}} X_{\text{hora}}
$$

* **Desempeño:**
  * **$D^2$ Score (Pseudo-R² de Poisson):** $0.7087$.

### 3. Dimensión Demográfica (Naive Bayes)
* **Auditoría y selección formal de variables:**
  * *Target Leakage:* Descarte de variables posteriores al hecho (incapacidad médico-legal, captura, mecanismo de lesión).
  * *Varianza y Entropía:* Descarte de variables con concentración superior al $85\,\%$ en una categoría.
  * *Incompletitud:* La localidad original en Medicina Legal presentaba $82.16\,\%$ de valores nulos, sustentando el cruce geográfico con la base distrital.
  * *Variables retenidas:* `Sexo de la víctima` y `Ciclo Vital` (completitud superior al $99.9\,\%$).
* **Validación inferencial:**
  * Prueba $\chi^2$ de bondad de ajuste contra censo DANE ($48\,\%$ hombres / $52\,\%$ mujeres, $p < 0.0001$).
  * Cálculo de multiplicadores mediante razones de verosimilitud (*Likelihood Ratios*).

### 4. Ensamble del Score Final
Dada una coordenada geográfica, el motor ubica el sector catastral más próximo evaluando la distancia geodésica de Haversine:

$$
d = 2 R \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)
$$

El puntaje consolidado de riesgo se calcula combinando los factores:

$$
\text{Riesgo Final} = \text{Score Base}_{\text{Cluster}} \times \left( 0.65 \cdot (\text{Factor}_{\text{Mes}} \cdot \text{Factor}_{\text{Dia}} \cdot \text{Factor}_{\text{Hora}}) + 0.35 \cdot (0.55 \cdot \text{Factor}_{\text{Sexo}} + 0.45 \cdot \text{Factor}_{\text{Edad}}) \right)
$$

El índice resultante se restringe al intervalo $[5.0, 99.0]$ y se clasifica operativamente en:
- 🟢 **Bajo ($< 45.0$):** Condiciones favorables en el sector y franja horaria.
- 🟠 **Moderado ($45.0 - 69.9$):** Precaución activa en vía pública.
- 🔴 **Crítico / Alto ($\ge 70.0$):** Alerta máxima por alta incidencia delictiva y temporal.

---

## 📱 Modos de la Aplicación Móvil

La aplicación Android no utiliza coordenadas simuladas ni valores predeterminados en duro. Toda la inferencia se desprende de funciones y estados verificables:

1. **Modo 1: Mi Estado Actual (GPS en Vivo + Perfil Guardado)**
   - **Gestión de Permisos:** Solicita en tiempo de ejecución la autorización para el sensor de ubicación (`ACCESS_FINE_LOCATION`).
   - **Perfil Demográfico:** Lee el sexo y edad guardados localmente mediante preferencias privadas. Si es el primer inicio, despliega un formulario obligatorio.
   - **Inferencia en Tiempo Real:** Obtiene la coordenada satelital activa, lee la hora y fecha del sistema y genera el diagnóstico del sector catastral donde se encuentra el usuario.

2. **Modo 2: Evaluar Estado Posible (Simulador Cartográfico)**
   - **Cartografía Interactiva (OSMDroid):** Permite explorar el mapa urbano de Bogotá y tocar cualquier punto geográfico para situar un marcador dinámico.
   - **Simulación Paramétrica:** Permite definir cualquier combinación de sexo, edad, día de la semana o franja horaria para evaluar el riesgo de trayectos o destinos antes de desplazarse.

---

## 🚀 Ejecución y Despliegue

### Fase 1: Pipeline de Datos (Podman / Docker)

El preprocesamiento, la auditoría estadística y el entrenamiento se ejecutan en un contenedor aislado y reproducible:

```bash
# Iniciar con Podman
podman-compose up --build

# Iniciar con Docker
docker compose up --build
```

1. Acceder al entorno de Jupyter Lab en: `http://127.0.0.1:8888`
2. Abrir y ejecutar las celdas de `Pipeline_KMeans_Poisson_Bayes_Bogota.ipynb`.
3. El proceso exportará automáticamente el archivo unificado `modelo_seguridad_bogota.json`.

### Fase 2: Aplicación Móvil (Instalación por APK)

Para demostraciones o uso en campo no se requiere compilar desde Android Studio:

1. Descargar o transferir el archivo `BogotaSafe.apk` al dispositivo móvil con Android 8.0 o superior.
2. Habilitar la opción de instalación de aplicaciones de fuentes desconocidas si el sistema lo requiere.
3. Abrir la app, otorgar los permisos de ubicación solicitados y configurar el perfil demográfico inicial.
