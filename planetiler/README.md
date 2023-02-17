# Planetiler - Loli Style

## Usage

Build Planetiler (Requires Java, see [Planetiler->Contributing](https://github.com/onthegomap/planetiler/blob/main/CONTRIBUTING.md):

```
./mvnw clean package --file standalone.pom.xml
```

Now you can run Planetiler. We recommend to start with a small area, for example Monaco (note while Monaco is small, we still need to separately download the roughly 1GB large `water-polygons-split-3857.zip` file):

```
java -cp target/*-with-deps.jar com.onthegomap.planetiler.examples.LoliStyle --maxzoom=15 --download --area=monaco
```

Inspect the tiles with tileserver-gl:

```
docker run --rm -it -v "$(pwd)/data":/data -p 8080:8080 maptiler/tileserver-gl -p 8080
```

