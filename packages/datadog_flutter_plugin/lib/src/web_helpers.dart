// Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
// This product includes software developed at Datadog (https://www.datadoghq.com/).
// Copyright 2019-Present Datadog, Inc.

import 'package:flutter/foundation.dart';
import 'package:js/js_util.dart' as jsutil;

import '../datadog_flutter_plugin.dart';

String siteStringForSite(DatadogSite? site) {
  switch (site) {
    case DatadogSite.us1:
      return 'datadoghq.com';
    case DatadogSite.us3:
      return 'us3.datadoghq.com';
    case DatadogSite.us5:
      return 'us5.datadoghq.com';
    case DatadogSite.eu1:
      return 'datadoghq.eu';
    case DatadogSite.us1Fed:
      return 'ddog-gov.com';
    default:
      return 'datadoghq.com';
  }
}

dynamic attributesToJs(Map<String, Object?> attributes, String parameterName) {
  return valueToJs(attributes, parameterName);
}

dynamic valueToJs(Object? value, String parameterName) {
  if (value == null || value is num || value is bool || value is String) {
    return value;
  }

  if (value is Map) {
    final jsMap = jsutil.newObject<Map<String, Object?>>();
    for (final item in value.entries) {
      jsutil.setProperty(
          jsMap, item.key, valueToJs(item.value, '$parameterName.${item.key}'));
    }
    return jsMap;
  }

  if (value is List) {
    final jsList = <Object?>[];
    for (int i = 0; i < value.length; ++i) {
      jsList.add(valueToJs(value[i], '$parameterName[$i]'));
    }
    return jsList;
  }

  throw ArgumentError(
      'Could not convert ${value.runtimeType} to javascript.', parameterName);
}

// Regex specifying the format of a frame in a Dart stack trace.
final _dartLineRegex =
    RegExp(r'(?<file>.+) (?<location>\d+:\d+)\s*(?<function>.+)');

String? convertWebStackTrace(StackTrace? stackTrace) {
  if (stackTrace == null) return null;

  var stackTraceString = stackTrace.toString();
  if (kDebugMode) {
    // Datadog Browser SDK parses the stack trace looking for specific
    // formats. When deployed, the Dart's StackTrace.toString will
    // correctly output a JS compatible stack trace. When not deployed,
    // we reformat so that it puts something in Datadog logging.
    var sb = StringBuffer();
    for (var line in stackTraceString.split('\n')) {
      var match = _dartLineRegex.firstMatch(line);
      if (match != null) {
        final file = match.namedGroup('file');
        final location = match.namedGroup('location');
        final function = match.namedGroup('function');
        sb.writeln('  at $function (file://$file:$location) ');
      }
    }
    stackTraceString = sb.toString();
  }

  return stackTraceString;
}
