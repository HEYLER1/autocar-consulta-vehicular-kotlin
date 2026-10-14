package com.example.app_autocar;

/** Immutable source metadata shared by Home and the directory. */
public final class OfficialSource {
  final String category;
  final String title;
  final String description;
  final String accessLabel;
  final String url;
  final String symbol;

  OfficialSource(
      String category,
      String title,
      String description,
      String accessLabel,
      String url,
      String symbol) {
    this.category = category;
    this.title = title;
    this.description = description;
    this.accessLabel = accessLabel;
    this.url = url;
    this.symbol = symbol;
  }
}
