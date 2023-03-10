package com.onthegomap.planetiler.examples;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;

import java.nio.file.Path;
import java.util.List;

public class LoeliStyle implements Profile {

  public static final String OCEAN_SOURCE = "ocean";
  public static final double MERGE_LINE_STRINGS_MIN_LENGTH = 0.5;
  public static final double MERGE_LINE_STRINGS_TOLERANCE = 0.5;
  public static final double MERGE_LINE_STRINGS_BUFFER = 4.0;
  public static final double MERGE_OVERLAPPING_POLYGONS_MIN_AREA = 4.0;

  @Override
  public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {

    // building layer
    if (sourceFeature.canBePolygon() && sourceFeature.hasTag("building") && !sourceFeature.hasTag("building", "no")) {
      features.polygon("building")
        .setMinZoom(15);
    }

    // boundary layer
    if (sourceFeature.canBeLine() && (
      sourceFeature.hasTag("boundary", "administrative") &&
      sourceFeature.hasTag("admin_level", "2") &&
      !sourceFeature.hasTag("maritime", "yes")
    )) {
      features.line("boundary")
        .setMinPixelSize(0)
        .setMinZoom(0);
    }

    // water layer
    if (sourceFeature.canBePolygon() && (
      sourceFeature.hasTag("natural", "water") ||
      sourceFeature.hasTag("waterway", 
        "riverbank",
        "dock",
        "canal"
      ) ||
      sourceFeature.hasTag("landuse",
        "reservoir",
        "basin"
      ) ||
      OCEAN_SOURCE.equals(sourceFeature.getSource())
    )) {
      int minZoom = 0;
      if (OCEAN_SOURCE.equals(sourceFeature.getSource())) {
        minZoom = 0;
      }
      else {
        if (sourceFeature.hasTag("waterway", "dock", "canal")) {
          minZoom = 10;
        }
        else {
          minZoom = 4;
        }
      }
      features.polygon("water")
        .setMinZoom(minZoom);
    }
  }

  @Override
  public List<VectorTile.Feature> postProcessLayerFeatures(String layer, int zoom,
    List<VectorTile.Feature> items) {

    if ("boundary".equals(layer)) {
      return FeatureMerge.mergeLineStrings(items,
        MERGE_LINE_STRINGS_MIN_LENGTH,
        MERGE_LINE_STRINGS_TOLERANCE,
        MERGE_LINE_STRINGS_BUFFER
      );
    }

    if ("water".equals(layer)) {
      try {
        return FeatureMerge.mergeOverlappingPolygons(items, MERGE_OVERLAPPING_POLYGONS_MIN_AREA);
      }
      catch (GeometryException e) {
        return null;
      }
    }

    return null;
  }

  @Override
  public String name() {
    return "loeli-style";
  }

  @Override
  public String description() {
    return "No map is perfect";
  }

  @Override
  public String attribution() {
    return "<a href=\"https://www.openstreetmap.org/copyright\" target=\"_blank\">&copy; OpenStreetMap contributors</a>";
  }

  public static void main(String[] args) throws Exception {
    run(Arguments.fromArgsOrConfigFile(args).orElse(Arguments.of("maxzoom", 15)));
  }

  static void run(Arguments args) throws Exception {
    String area = args.getString("area", "geofabrik area to download", "monaco");
    Planetiler.create(args) 
      .setProfile(new LoeliStyle())
      .addOsmSource("osm", Path.of("data", "sources", area + ".osm.pbf"), "planet".equals(area) ? "aws:latest" : ("geofabrik:" + area))
      .addShapefileSource(OCEAN_SOURCE, Path.of("data", "sources", "water-polygons-split-3857.zip"),
        "https://osmdata.openstreetmap.de/download/water-polygons-split-3857.zip")
      .overwriteOutput("mbtiles", Path.of("data", "loeli-style.mbtiles"))
      .run();
  }
}