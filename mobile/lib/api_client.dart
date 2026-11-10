import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api/v1',
);

class ApiException implements Exception {
  const ApiException(this.message, [this.statusCode]);
  final String message;
  final int? statusCode;
  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({http.Client? client, FlutterSecureStorage? storage})
      : _client = client ?? http.Client(),
        _storage = storage ?? const FlutterSecureStorage();

  final http.Client _client;
  final FlutterSecureStorage _storage;
  String? _accessToken;
  String? _refreshToken;

  bool get hasSession => _accessToken != null && _refreshToken != null;

  Future<void> restoreSession() async {
    _accessToken = await _storage.read(key: 'melarium.access');
    _refreshToken = await _storage.read(key: 'melarium.refresh');
  }

  Future<void> _saveSession(Map<String, dynamic> data) async {
    final access = data['accessToken'] as String?;
    final refresh = data['refreshToken'] as String?;
    if (access == null || refresh == null) {
      throw const ApiException('Kirish sessiyasini tiklab bo‘lmadi.');
    }
    _accessToken = access;
    _refreshToken = refresh;
    await _storage.write(key: 'melarium.access', value: access);
    await _storage.write(key: 'melarium.refresh', value: refresh);
  }

  Future<void> clearSession() async {
    _accessToken = null;
    _refreshToken = null;
    await _storage.delete(key: 'melarium.access');
    await _storage.delete(key: 'melarium.refresh');
  }

  Future<Map<String, dynamic>> requestOtp(String phone) async {
    return await _sendJson('POST', '/auth/otp/request',
        body: {'phone': phone}, authenticated: false) as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> verifyOtp(String phone, String code) async {
    final response = await _sendJson('POST', '/auth/otp/verify',
        body: {'phone': phone, 'code': code},
        authenticated: false) as Map<String, dynamic>;
    await _saveSession(response);
    return response;
  }

  Future<Map<String, dynamic>> me() async =>
      await _sendJson('GET', '/users/me') as Map<String, dynamic>;

  Future<Map<String, dynamic>> get(String path) async =>
      await _sendJson('GET', path) as Map<String, dynamic>;

  Future<List<dynamic>> getList(String path) async =>
      await _sendJson('GET', path) as List<dynamic>;

  Future<Map<String, dynamic>> post(String path,
          [Map<String, dynamic>? body]) async =>
      await _sendJson('POST', path, body: body) as Map<String, dynamic>;

  Future<Map<String, dynamic>> put(
          String path, Map<String, dynamic> body) async =>
      await _sendJson('PUT', path, body: body) as Map<String, dynamic>;

  Future<Map<String, dynamic>> patch(
          String path, Map<String, dynamic> body) async =>
      await _sendJson('PATCH', path, body: body) as Map<String, dynamic>;

  Future<void> delete(String path) async {
    await _sendJson('DELETE', path);
  }

  Future<void> logout() async {
    final refresh = _refreshToken;
    try {
      if (refresh != null) {
        await _sendJson('POST', '/auth/logout',
            body: {'refreshToken': refresh},
            authenticated: false,
            retryRefresh: false);
      }
    } finally {
      await clearSession();
    }
  }

  Future<dynamic> _sendJson(
    String method,
    String path, {
    Map<String, dynamic>? body,
    bool authenticated = true,
    bool retryRefresh = true,
  }) async {
    final uri = Uri.parse('$apiBaseUrl$path');
    final request = http.Request(method, uri)
      ..headers['Accept'] = 'application/json'
      ..headers['Content-Type'] = 'application/json';
    if (authenticated && _accessToken != null) {
      request.headers['Authorization'] = 'Bearer $_accessToken';
    }
    if (body != null) request.body = jsonEncode(body);

    http.Response response;
    try {
      response = await http.Response.fromStream(await _client.send(request));
    } catch (_) {
      throw const ApiException(
          'So‘rovni bajarib bo‘lmadi.');
    }

    if (response.statusCode == 401 &&
        authenticated &&
        retryRefresh &&
        await _refresh()) {
      return _sendJson(method, path,
          body: body, authenticated: authenticated, retryRefresh: false);
    }
    if (response.statusCode == 204 || response.body.isEmpty) return null;

    dynamic payload;
    try {
      payload = jsonDecode(response.body);
    } catch (_) {
      payload = response.body;
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message =
          payload is Map<String, dynamic> && payload['message'] is String
              ? payload['message'] as String
              : 'So‘rov bajarilmadi (${response.statusCode})';
      final safeMessage = RegExp(
                  r'\b(?:api|backend|server|postgres(?:ql)?|database|connection|connected)\b',
                  caseSensitive: false)
              .hasMatch(message)
          ? 'So‘rovni bajarib bo‘lmadi.'
          : message;
      throw ApiException(safeMessage, response.statusCode);
    }
    return payload;
  }

  Future<bool> _refresh() async {
    final refresh = _refreshToken;
    if (refresh == null) return false;
    try {
      final response = await _sendJson('POST', '/auth/token/refresh',
          body: {'refreshToken': refresh},
          authenticated: false,
          retryRefresh: false);
      if (response is! Map<String, dynamic>) return false;
      await _saveSession(response);
      return true;
    } catch (_) {
      await clearSession();
      return false;
    }
  }
}
