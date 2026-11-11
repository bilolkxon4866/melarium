import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';

import 'api_client.dart';

const forest = Color(0xFF28543A);
const leaf = Color(0xFF4B8557);
const canvas = Color(0xFFF5F7F3);
const ink = Color(0xFF25362B);

void main() => runApp(const MelariumApp());

class MelariumApp extends StatelessWidget {
  const MelariumApp({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme =
        ColorScheme.fromSeed(seedColor: forest, surface: Colors.white);
    return MaterialApp(
      title: 'MELARIUM',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: scheme,
        scaffoldBackgroundColor: canvas,
        appBarTheme: const AppBarTheme(
          backgroundColor: canvas,
          foregroundColor: ink,
          elevation: 0,
          centerTitle: false,
          titleTextStyle:
              TextStyle(color: ink, fontSize: 18, fontWeight: FontWeight.w800),
        ),
        cardTheme: CardThemeData(
          color: Colors.white,
          elevation: 0,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          margin: EdgeInsets.zero,
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Colors.white,
          border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Color(0xFFE1E8E1))),
          enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Color(0xFFE1E8E1))),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 13, vertical: 12),
        ),
      ),
      home: const MobileApp(),
    );
  }
}

class MobileApp extends StatefulWidget {
  const MobileApp({super.key});

  @override
  State<MobileApp> createState() => _MobileAppState();
}

class _MobileAppState extends State<MobileApp> {
  final _api = ApiClient();
  Map<String, dynamic>? _profile;
  bool _loading = true;
  String? _restoreError;

  @override
  void initState() {
    super.initState();
    _restore();
  }

  Future<void> _restore() async {
    try {
      await _api.restoreSession();
      if (_api.hasSession) _profile = await _api.me();
    } catch (error) {
      _restoreError = error.toString();
      await _api.clearSession();
      _profile = null;
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _authenticated(Map<String, dynamic> user) {
    setState(() {
      _profile = user;
      _restoreError = null;
    });
  }

  Future<void> _logout() async {
    await _api.logout();
    if (mounted) setState(() => _profile = null);
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(
          body: Center(child: CircularProgressIndicator(color: leaf)));
    }
    if (_profile == null) {
      return _GuestHome(
        api: _api,
        restoreError: _restoreError,
        onAuthenticated: _authenticated,
      );
    }
    return _MobileRoleShell(api: _api, profile: _profile!, onLogout: _logout);
  }
}

class _GuestHome extends StatefulWidget {
  const _GuestHome(
      {required this.api, required this.onAuthenticated, this.restoreError});
  final ApiClient api;
  final void Function(Map<String, dynamic>) onAuthenticated;
  final String? restoreError;

  @override
  State<_GuestHome> createState() => _GuestHomeState();
}

class _GuestHomeState extends State<_GuestHome> {
  late Future<Map<String, dynamic>> _listings;

  @override
  void initState() {
    super.initState();
    _listings = widget.api.get('/listings?page=0&size=30');
  }

  Future<void> _reload() async {
    setState(() => _listings = widget.api.get('/listings?page=0&size=30'));
    await _listings;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const _BrandTitle(),
        actions: [
          TextButton(onPressed: () => _openAuth(), child: const Text('Kirish'))
        ],
      ),
      body: FutureBuilder<Map<String, dynamic>>(
        future: _listings,
        builder: (context, snapshot) {
          final items = mapList(snapshot.data?['content'])
              .where((item) => item['status'] == 'ACTIVE')
              .toList();
          return RefreshIndicator(
            onRefresh: _reload,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
              children: [
                if (widget.restoreError != null)
                  const _ErrorBanner(
                      message:
                          'Oldingi sessiya qayta tiklanmadi. Qayta kiring.'),
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(22),
                    gradient: const LinearGradient(
                        colors: [Color(0xFF214B35), Color(0xFF568454)],
                        begin: Alignment.bottomLeft,
                        end: Alignment.topRight),
                  ),
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const _Eyebrow('ASALARICHILIK EKOTIZIMI'),
                        const SizedBox(height: 12),
                        const Text(
                            'Tabiat ne’matini yangi imkoniyatga aylantiring.',
                            style: TextStyle(
                                color: Colors.white,
                                fontSize: 25,
                                fontWeight: FontWeight.w800,
                                height: 1.15)),
                        const SizedBox(height: 10),
                        const Text(
                            'Faol takliflarni ko‘ring, hisob yarating yoki yordam xizmatiga murojaat yuboring.',
                            style: TextStyle(
                                color: Color(0xFFDCE8DC), height: 1.45)),
                        const SizedBox(height: 18),
                        FilledButton.icon(
                            onPressed: _openAuth,
                            icon: const Icon(Icons.login_rounded),
                            label: const Text('Kirish / ro‘yxatdan o‘tish')),
                      ]),
                ),
                const SizedBox(height: 18),
                _SectionHeader(
                    title: 'Faol takliflar',
                    subtitle: snapshot.hasError
                        ? 'Takliflar hozircha ko‘rinmadi'
                        : '${snapshot.data?['totalElements'] ?? 0} ta taklif',
                const SizedBox(height: 10),
                if (snapshot.connectionState == ConnectionState.waiting)
                  const Padding(
                      padding: EdgeInsets.all(24),
                      child:
                          Center(child: CircularProgressIndicator(color: leaf)))
                else if (snapshot.hasError)
                  const _EmptyCard(
                      icon: Icons.cloud_off_outlined,
                      title: 'Takliflarni yuklab bo‘lmadi',
                      text:
                          'Pastga tortib qayta urinib ko‘ring.')
                else if (items.isEmpty)
                  const _EmptyCard(
                      icon: Icons.spa_outlined,
                      title: 'Faol taklif hozircha yo‘q',
                      text:
                          'Tasdiqlangan takliflar ko‘paygach bu yerda ko‘rinadi.')
                else
                  ...items.map((item) => Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _ListingCard(
                          item: item, onTap: () => _showListing(item)))),
                const SizedBox(height: 12),
                Card(
                    child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const _SectionHeader(
                                  title: 'Savol-javob',
                                  subtitle: 'Muhim ma’lumotlar'),
                              const SizedBox(height: 10),
                              const _FaqItem(
                                  question:
                                      'Sensor ko‘rsatkichlari avtomatik keladimi?',
                                  answer:
                                      'Hozir mobil/web ilovada qo‘lda kiritilgan o‘lchovlar MANUAL deb belgilanadi; ular IoT telemetriyasi emas.'),
                              _FaqItem(
                                  question:
                                      'Yordam yoki shikoyatni qayerga yuboraman?',
                                  answer:
                                      'Kirishdan so‘ng “Yordam va murojaatlar” bo‘limida umumiy savol, texnik muammo yoki shikoyat yuboring.',
                                  onTap: _openAuth),
                            ]))),
                const SizedBox(height: 12),
              ],
            ),
          );
        },
      ),
    );
  }

  void _openAuth() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (context) =>
          _AuthSheet(api: widget.api, onAuthenticated: widget.onAuthenticated),
    );
  }

  void _showListing(Map<String, dynamic> item) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (context) =>
          _ListingDetails(item: item, isInvestor: false, onReserve: null),
    );
  }
}

class _AuthSheet extends StatefulWidget {
  const _AuthSheet({required this.api, required this.onAuthenticated});
  final ApiClient api;
  final void Function(Map<String, dynamic>) onAuthenticated;

  @override
  State<_AuthSheet> createState() => _AuthSheetState();
}

class _AuthSheetState extends State<_AuthSheet> {
  final _phone = TextEditingController();
  final _code = TextEditingController();
  bool _codeSent = false;
  bool _busy = false;
  String? _error;
  String? _debugCode;

  @override
  void dispose() {
    _phone.dispose();
    _code.dispose();
    super.dispose();
  }

  String get _normalizedPhone {
    var digits = _phone.text.replaceAll(RegExp(r'\D'), '');
    if (digits.startsWith('998')) digits = digits.substring(3);
    return '+998$digits';
  }

  Future<void> _requestCode() async {
    if (_normalizedPhone.length < 13) {
      setState(() => _error = 'To‘liq O‘zbekiston telefon raqamini kiriting.');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final result = await widget.api.requestOtp(_normalizedPhone);
      if (!mounted) return;
      setState(() {
        _codeSent = true;
        _debugCode = result['debugCode'] as String?;
      });
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _verify() async {
    if (_code.text.trim().length != 6) {
      setState(() => _error = '6 xonali kodni kiriting.');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.verifyOtp(_normalizedPhone, _code.text.trim());
      final profile = await widget.api.me();
      if (!mounted) return;
      Navigator.of(context).pop();
      widget.onAuthenticated(profile);
    } catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
          20, 16, 20, MediaQuery.viewInsetsOf(context).bottom + 22),
      child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const _SectionHeader(
                title: 'MELARIUM hisobiga kirish',
                subtitle: 'Telefon orqali bir martalik OTP'),
            const SizedBox(height: 16),
            if (!_codeSent) ...[
              TextField(
                  controller: _phone,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                      labelText: 'Telefon raqam',
                      prefixText: '+998 ',
                      hintText: '90 123 45 67')),
              const SizedBox(height: 12),
              FilledButton(
                  onPressed: _busy ? null : _requestCode,
                  child: _busy
                      ? const _ButtonProgress()
                      : const Text('Tasdiqlash kodini yuborish')),
            ] else ...[
              Text('Kod $_normalizedPhone raqamiga yuborildi.',
                  style: const TextStyle(color: Color(0xFF66756B))),
              if (_debugCode != null)
                Padding(
                    padding: const EdgeInsets.only(top: 10),
                    child: _WarningCard(
                        text:
                            'Development OTP: $_debugCode. Faqat local test uchun; production’da bu kod ochiq qaytarilmaydi.')),
              const SizedBox(height: 12),
              TextField(
                  controller: _code,
                  autofocus: true,
                  keyboardType: TextInputType.number,
                  maxLength: 6,
                  decoration:
                      const InputDecoration(labelText: '6 xonali OTP kod')),
              const SizedBox(height: 8),
              FilledButton(
                  onPressed: _busy ? null : _verify,
                  child: _busy
                      ? const _ButtonProgress()
                      : const Text('Tasdiqlash va kirish')),
              TextButton(
                  onPressed: _busy
                      ? null
                      : () => setState(() {
                            _codeSent = false;
                            _debugCode = null;
                            _error = null;
                          }),
                  child: const Text('Telefon raqamni o‘zgartirish')),
            ],
            if (_error != null)
              Padding(
                  padding: const EdgeInsets.only(top: 10),
                  child: _ErrorBanner(message: _error!)),
            const SizedBox(height: 8),
            const Text('OTP yoki sessiya tokenini hech kimga bermang.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFF89958C), fontSize: 11)),
          ]),
    );
  }
}

class _MobileRoleShell extends StatefulWidget {
  const _MobileRoleShell(
      {required this.api, required this.profile, required this.onLogout});
  final ApiClient api;
  final Map<String, dynamic> profile;
  final Future<void> Function() onLogout;

  @override
  State<_MobileRoleShell> createState() => _MobileRoleShellState();
}

class _MobileRoleShellState extends State<_MobileRoleShell> {
  late String _role;
  String _page = 'overview';
  Map<String, dynamic> _data = {};
  bool _loading = true;

  List<String> get _roles =>
      ((widget.profile['roles'] as List?) ?? const <dynamic>[])
          .map((value) => value.toString())
          .where((role) => const {
                'USER',
                'INVESTOR',
                'BEEKEEPER',
                'ADMIN',
                'SUPER_ADMIN'
              }.contains(role))
          .toList();

  List<_PageSpec> get _allPages {
    final common = <_PageSpec>[
      const _PageSpec('overview', 'Bosh sahifa', Icons.grid_view_rounded),
      const _PageSpec('marketplace', 'Takliflar bozori', Icons.spa_outlined),
      const _PageSpec('notifications', 'Bildirishnomalar',
          Icons.notifications_none_rounded),
      const _PageSpec(
          'support', 'Yordam va murojaatlar', Icons.support_agent_rounded),
      const _PageSpec('profile', 'Profil', Icons.person_outline_rounded),
      const _PageSpec(
          'settings', 'Bildirishnoma sozlamalari', Icons.settings_outlined),
    ];
    if (_role == 'USER') return common;
    if (_role == 'INVESTOR') {
      return [
        ...common,
        const _PageSpec('investments', 'Investitsiyalarim',
            Icons.account_balance_wallet_outlined),
        const _PageSpec('payments', 'To‘lovlarim', Icons.credit_card_outlined),
        const _PageSpec('reports', 'Hisobotlar', Icons.bar_chart_rounded)
      ];
    }
    if (_role == 'BEEKEEPER') {
      return [
        ...common,
        const _PageSpec(
            'apiaries', 'Asalarixonalar va ari oilalari', Icons.hive_outlined),
        const _PageSpec(
            'harvests', 'Hosil va xarajatlar', Icons.inventory_2_outlined),
        const _PageSpec(
            'sensors', 'Qo‘lda sensor qaydi', Icons.thermostat_outlined),
        const _PageSpec('reports', 'Hisobotlar', Icons.bar_chart_rounded)
      ];
    }
    return [
      ...common,
      const _PageSpec('moderation', 'Takliflar moderatsiyasi',
          Icons.verified_user_outlined),
      const _PageSpec('users', 'Foydalanuvchilar va rollar',
          Icons.manage_accounts_outlined),
      const _PageSpec(
          'harvests', 'Hosil moderatsiyasi', Icons.inventory_2_outlined),
      const _PageSpec('sensors', 'Sensor qaydlari', Icons.thermostat_outlined),
      const _PageSpec('payments', 'To‘lovlar', Icons.credit_card_outlined),
      const _PageSpec('audit', 'Audit jurnali', Icons.receipt_long_outlined),
    ];
  }

  List<String> get _bottomPages {
    if (_role == 'USER') {
      return const ['overview', 'marketplace', 'support', 'profile'];
    }
    if (_role == 'INVESTOR') {
      return const ['overview', 'marketplace', 'investments', 'profile'];
    }
    if (_role == 'BEEKEEPER') {
      return const ['overview', 'apiaries', 'harvests', 'profile'];
    }
    return const ['overview', 'moderation', 'users', 'profile'];
  }

  @override
  void initState() {
    super.initState();
    _role = _initialRole(_roles);
    _load();
  }

  String _initialRole(List<String> roles) {
    if (roles.contains('SUPER_ADMIN') || roles.contains('ADMIN')) {
      return roles.contains('SUPER_ADMIN') ? 'SUPER_ADMIN' : 'ADMIN';
    }
    if (roles.contains('BEEKEEPER')) return 'BEEKEEPER';
    if (roles.contains('INVESTOR')) return 'INVESTOR';
    return 'USER';
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
    });
    Future<T> safe<T>(Future<T> Function() call, T fallback) async {
      try {
        return await call();
      } catch (_) {
        return fallback;
      }
    }

    final data = <String, dynamic>{};
    final listingPage = await safe(
        () => widget.api.get('/listings?page=0&size=100'),
        <String, dynamic>{'content': <dynamic>[], 'totalElements': 0});
    data['listingPage'] = listingPage;
    data['listings'] = mapList(listingPage['content']);

    if (_role == 'USER') {
      final results = await Future.wait<dynamic>([
        safe(() => widget.api.get('/notifications?page=0&size=100'),
            <String, dynamic>{'content': <dynamic>[]}),
        safe(() => widget.api.get('/support/tickets?page=0&size=100'),
            <String, dynamic>{'content': <dynamic>[]}),
        safe(() => widget.api.get('/notifications/preferences'),
            <String, dynamic>{}),
      ]);
      data['notifications'] = mapList(asMap(results[0])['content']);
      data['tickets'] = mapList(asMap(results[1])['content']);
      data['preferences'] = asMap(results[2]);
    } else if (_role == 'INVESTOR') {
      final results = await Future.wait<dynamic>([
        safe(() => widget.api.getList('/investments/my'), <dynamic>[]),
        safe(() => widget.api.get('/reports/investor'), <String, dynamic>{}),
        safe(() => widget.api.get('/payments/my?page=0&size=100'),
            <String, dynamic>{'content': <dynamic>[]}),
        safe(() => widget.api.get('/notifications?page=0&size=100'),
            <String, dynamic>{'content': <dynamic>[]}),
        safe(() => widget.api.get('/support/tickets?page=0&size=100'),
            <String, dynamic>{'content': <dynamic>[]}),
        safe(() => widget.api.get('/notifications/preferences'),
            <String, dynamic>{}),
      ]);
      data['investments'] = mapList(results[0]);
      data['investorReport'] = asMap(results[1]);
      data['payments'] = mapList(asMap(results[2])['content']);
      data['notifications'] = mapList(asMap(results[3])['content']);
      data['tickets'] = mapList(asMap(results[4])['content']);
      data['preferences'] = asMap(results[5]);
    } else if (_role == 'BEEKEEPER' ||
        _role == 'ADMIN' ||
        _role == 'SUPER_ADMIN') {
      final apiaryRoute = _role == 'BEEKEEPER' ? '/apiaries' : '/apiaries';
      final apiaries =
          await safe(() => widget.api.getList(apiaryRoute), <dynamic>[]);
      data['apiaries'] = mapList(apiaries);
      final colonies = <Map<String, dynamic>>[];
      final sensors = <Map<String, dynamic>>[];
      for (final apiary in mapList(apiaries)) {
        final id = apiary['id']?.toString();
        if (id == null) continue;
        final apiaryColonies = await safe(
            () => widget.api.getList('/colonies?apiaryId=$id'), <dynamic>[]);
        colonies.addAll(mapList(apiaryColonies));
        final page = await safe(
            () =>
                widget.api.get('/sensor-readings?apiaryId=$id&page=0&size=100'),
            <String, dynamic>{'content': <dynamic>[]});
        sensors.addAll(mapList(page['content']));
      }
      data['colonies'] = colonies;
      data['sensors'] = sensors;
      final common = await Future.wait<dynamic>([
        safe(() => widget.api.getList('/harvests'), <dynamic>[]),
        safe(() => widget.api.getList('/expenses'), <dynamic>[]),
        safe(() => widget.api.get('/notifications?page=0&size=100'),
            <String, dynamic>{'content': <dynamic>[]}),
        safe(() => widget.api.get('/support/tickets?page=0&size=100'),
            <String, dynamic>{'content': <dynamic>[]}),
        safe(() => widget.api.get('/notifications/preferences'),
            <String, dynamic>{}),
      ]);
      data['harvests'] = mapList(common[0]);
      data['expenses'] = mapList(common[1]);
      data['notifications'] = mapList(asMap(common[2])['content']);
      data['tickets'] = mapList(asMap(common[3])['content']);
      data['preferences'] = asMap(common[4]);
      if (_role == 'BEEKEEPER') {
        data['beekeeperReport'] = await safe(
            () => widget.api.get('/reports/beekeeper'), <String, dynamic>{});
      } else {
        final admin = await Future.wait<dynamic>([
          safe(() => widget.api.get('/admin/dashboard'), <String, dynamic>{}),
          safe(() => widget.api.get('/admin/users?page=0&size=100'),
              <String, dynamic>{'content': <dynamic>[]}),
          safe(() => widget.api.get('/listings/moderation?page=0&size=100'),
              <String, dynamic>{'content': <dynamic>[]}),
          safe(() => widget.api.get('/payments/admin?page=0&size=100'),
              <String, dynamic>{'content': <dynamic>[]}),
          safe(() => widget.api.get('/admin/investments?page=0&size=100'),
              <String, dynamic>{'content': <dynamic>[]}),
          safe(() => widget.api.get('/admin/audit-logs?page=0&size=100'),
              <String, dynamic>{'content': <dynamic>[]}),
          safe(() => widget.api.get('/admin/support/tickets?page=0&size=100'),
              <String, dynamic>{'content': <dynamic>[]}),
        ]);
        data['adminDashboard'] = asMap(admin[0]);
        data['users'] = mapList(asMap(admin[1])['content']);
        data['moderation'] = mapList(asMap(admin[2])['content']);
        data['payments'] = mapList(asMap(admin[3])['content']);
        data['investments'] = mapList(asMap(admin[4])['content']);
        data['auditLogs'] = mapList(asMap(admin[5])['content']);
        data['tickets'] = mapList(asMap(admin[6])['content']);
      }
    }
    if (!mounted) return;
    setState(() {
      _data = data;
      _loading = false;
    });
  }

  void _go(String page) {
    Navigator.of(context).maybePop();
    setState(() => _page = page);
  }

  Future<void> _switchRole(String? next) async {
    if (next == null) return;
    setState(() {
      _role = next;
      _page = 'overview';
    });
    await _load();
  }

  String get _roleLabel => switch (_role) {
        'USER' => 'Oddiy foydalanuvchi',
        'INVESTOR' => 'Investor',
        'BEEKEEPER' => 'Asalarichi',
        'SUPER_ADMIN' => 'Super Admin',
        _ => 'Administrator',
      };

  String get _pageTitle =>
      _allPages.where((item) => item.id == _page).firstOrNull?.title ??
      'MELARIUM';

  @override
  Widget build(BuildContext context) {
    final bottom = _bottomPages;
    final selectedBottom = bottom.indexOf(_page);
    return Scaffold(
      appBar: AppBar(
        title:
            _BrandTitle(label: _page == 'overview' ? 'MELARIUM' : _pageTitle),
        actions: [
          if (_roles.length > 1)
            Padding(
                padding: const EdgeInsets.only(right: 2),
                child: DropdownButtonHideUnderline(
                    child: DropdownButton<String>(
                        value: _role,
                        icon: const Icon(Icons.expand_more_rounded),
                        items: _roles
                            .map((role) => DropdownMenuItem(
                                value: role,
                                child: Text(
                                    role == 'SUPER_ADMIN'
                                        ? 'Super Admin'
                                        : role == 'ADMIN'
                                            ? 'Admin'
                                            : role,
                                    style: const TextStyle(fontSize: 11))))
                            .toList(),
                        onChanged: _switchRole))),
          IconButton(
              tooltip: 'Bildirishnomalar',
              onPressed: () => _go('notifications'),
              icon: const Icon(Icons.notifications_none_rounded)),
        ],
      ),
      drawer: Drawer(
          child: SafeArea(
              child: Column(children: [
        ListTile(
            leading: const CircleAvatar(
                backgroundColor: Color(0xFFE9F2E9),
                child: Icon(Icons.eco_rounded, color: forest)),
            title: Text(displayName(widget.profile)),
            subtitle: Text(_roleLabel),
            trailing: IconButton(
                onPressed: widget.onLogout,
                icon: const Icon(Icons.logout_rounded),
                tooltip: 'Chiqish')),
        const Divider(),
        Expanded(
            child: ListView(
                children: _allPages
                    .map((page) => ListTile(
                        leading: Icon(page.icon),
                        title: Text(page.title),
                        selected: _page == page.id,
                        onTap: () => _go(page.id)))
                    .toList())),
      ]))),
      body: Column(children: [
        if (_loading)
          const LinearProgressIndicator(
              minHeight: 2, color: leaf, backgroundColor: Color(0xFFEAF1E9)),
        Expanded(
            child: RefreshIndicator(onRefresh: _load, child: _buildPage())),
      ]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: selectedBottom < 0 ? 0 : selectedBottom,
        onDestinationSelected: (index) => setState(() => _page = bottom[index]),
        destinations: bottom.map((id) {
          final spec = _allPages.where((item) => item.id == id).firstOrNull ??
              const _PageSpec(
                  'overview', 'Bosh sahifa', Icons.grid_view_rounded);
          return NavigationDestination(
              icon: Icon(spec.icon),
              label: spec.title.length > 13
                  ? spec.title.substring(0, 12)
                  : spec.title);
        }).toList(),
      ),
    );
  }

  Widget _buildPage() {
    final listings = mapList(_data['listings']);
    switch (_page) {
      case 'overview':
        return _OverviewPage(
            role: _role, profile: widget.profile, data: _data, onNavigate: _go);
      case 'marketplace':
        return _MarketplacePage(
            api: widget.api,
            items: listings,
            role: _role,
            onChanged: _load,
            onMessage: _message);
      case 'investments':
        return _InvestmentsPage(
            api: widget.api,
            investments: mapList(_data['investments']),
            onChanged: _load,
            onMessage: _message);
      case 'payments':
        return _PaymentsPage(
            payments: mapList(_data['payments']),
            admin: _role == 'ADMIN' || _role == 'SUPER_ADMIN');
      case 'apiaries':
        return _ApiariesPage(
            api: widget.api,
            apiaries: mapList(_data['apiaries']),
            colonies: mapList(_data['colonies']),
            onChanged: _load,
            onMessage: _message);
      case 'harvests':
        return _HarvestsPage(
            api: widget.api,
            apiaries: mapList(_data['apiaries']),
            harvests: mapList(_data['harvests']),
            expenses: mapList(_data['expenses']),
            admin: _role == 'ADMIN' || _role == 'SUPER_ADMIN',
            onChanged: _load,
            onMessage: _message);
      case 'sensors':
        return _SensorsPage(
            api: widget.api,
            apiaries: mapList(_data['apiaries']),
            colonies: mapList(_data['colonies']),
            readings: mapList(_data['sensors']),
            onChanged: _load,
            onMessage: _message);
      case 'moderation':
        return _ModerationPage(
            api: widget.api,
            items: mapList(_data['moderation']),
            onChanged: _load,
            onMessage: _message);
      case 'users':
        return _UsersPage(
            api: widget.api,
            users: mapList(_data['users']),
            canAssignAdmin: _roles.contains('SUPER_ADMIN'),
            onChanged: _load,
            onMessage: _message);
      case 'support':
        return _SupportPage(
            api: widget.api,
            tickets: mapList(_data['tickets']),
            admin: _role == 'ADMIN' || _role == 'SUPER_ADMIN',
            onChanged: _load,
            onMessage: _message);
      case 'notifications':
        return _NotificationsPage(
            api: widget.api,
            notifications: mapList(_data['notifications']),
            onChanged: _load,
            onMessage: _message);
      case 'settings':
        return _SettingsPage(
            api: widget.api,
            preferences: asMap(_data['preferences']),
            onChanged: _load,
            onMessage: _message);
      case 'reports':
        return _ReportsPage(
            role: _role,
            report: asMap(_data['investorReport']).isNotEmpty
                ? asMap(_data['investorReport'])
                : asMap(_data['beekeeperReport']),
            data: _data);
      case 'audit':
        return _AuditPage(rows: mapList(_data['auditLogs']));
      case 'profile':
        return _ProfilePage(
            api: widget.api,
            profile: widget.profile,
            role: _role,
            onLogout: widget.onLogout,
            onChanged: _load,
            onMessage: _message);
      default:
        return const _EmptyCard(
            icon: Icons.info_outline,
            title: 'Bo‘lim topilmadi',
            text: 'Navigatsiya menyusidan boshqa sahifa tanlang.');
    }
  }

  void _message(String message) {
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }
}

class _OverviewPage extends StatelessWidget {
  const _OverviewPage(
      {required this.role,
      required this.profile,
      required this.data,
      required this.onNavigate});
  final String role;
  final Map<String, dynamic> profile;
  final Map<String, dynamic> data;
  final void Function(String) onNavigate;

  @override
  Widget build(BuildContext context) {
    final listings = mapList(data['listings'])
        .where((item) => item['status'] == 'ACTIVE')
        .toList();
    final investments = mapList(data['investments']);
    final apiaries = mapList(data['apiaries']);
    final colonies = mapList(data['colonies']);
    final harvests = mapList(data['harvests']);
    final expenses = mapList(data['expenses']);
    final dashboard = asMap(data['adminDashboard']);
    final investorReport = asMap(data['investorReport']);
    final beekeeperReport = asMap(data['beekeeperReport']);
    final tickets = mapList(data['tickets']);
    final isUser = role == 'USER';
    final isInvestor = role == 'INVESTOR';
    final isKeeper = role == 'BEEKEEPER';
    final stats = isUser
        ? <(String, String, IconData)>[
            ('Faol takliflar', '${listings.length}', Icons.spa_outlined),
            (
              'Bildirishnomalar',
              '${mapList(data['notifications']).length}',
              Icons.notifications_none_rounded
            ),
            ('Murojaatlar', '${tickets.length}', Icons.support_agent_rounded),
          ]
        : isInvestor
            ? <(String, String, IconData)>[
                (
                  'Faol buyurtma',
                  '${investorReport['activeInvestments'] ?? investments.where((item) => item['status'] == 'ACTIVE').length}',
                  Icons.account_balance_wallet_outlined
                ),
                (
                  'Kutilayotgan',
                  '${investorReport['pendingOrders'] ?? investments.where((item) => item['status'] == 'PENDING_PAYMENT').length}',
                  Icons.schedule_rounded
                ),
                (
                  'Faol buyurtmalar qiymati',
                  formatMoney(
                      numberValue(investorReport['activePrincipalAmount'])),
                  Icons.savings_outlined
                ),
              ]
            : isKeeper
                ? <(String, String, IconData)>[
                    (
                      'Asalarixonalar',
                      '${beekeeperReport['apiaryCount'] ?? apiaries.length}',
                      Icons.hive_outlined
                    ),
                    (
                      'Ari oilalari',
                      '${beekeeperReport['colonyCount'] ?? colonies.fold<int>(0, (sum, item) => sum + intValue(item['count']))}',
                      Icons.grid_view_rounded
                    ),
                    (
                      'Hosil',
                      '${numberValue(beekeeperReport['harvestKg']).toStringAsFixed(1)} kg',
                      Icons.water_drop_outlined
                    ),
                    (
                      'Xarajat',
                      formatMoney(numberValue(beekeeperReport['expensesUzs'])),
                      Icons.receipt_long_outlined
                    ),
                  ]
                : <(String, String, IconData)>[
                    (
                      'Foydalanuvchilar',
                      '${dashboard['totalUsers'] ?? mapList(data['users']).length}',
                      Icons.people_outline_rounded
                    ),
                    (
                      'Moderatsiya',
                      '${dashboard['pendingListings'] ?? mapList(data['moderation']).length}',
                      Icons.verified_user_outlined
                    ),
                    (
                      'Faol takliflar',
                      '${dashboard['activeListings'] ?? listings.length}',
                      Icons.spa_outlined
                    ),
                    (
                      'To‘lovlar',
                      '${mapList(data['payments']).length}',
                      Icons.credit_card_outlined
                    ),
                  ];
    return ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 26),
        children: [
          _Eyebrow('${roleLabel(role).toUpperCase()} KABINETI'),
          const SizedBox(height: 6),
          Text(
              role == 'ADMIN' || role == 'SUPER_ADMIN'
                  ? 'Platforma holati'
                  : 'Xush kelibsiz, ${displayName(profile)}',
              style: const TextStyle(
                  color: ink, fontSize: 23, fontWeight: FontWeight.w800)),
          const SizedBox(height: 6),
          Text(
              isUser
                  ? 'Oddiy foydalanuvchi hisobi. Investor yoki asalarichi rolini administrator vakolatli jarayon orqali tayinlaydi.'
                  : isInvestor
                      ? 'Buyurtmalar va xo‘jalik hisobotlari.'
                      : isKeeper
                          ? 'Xo‘jalik, ari oilalari, hosil va xarajatlarni boshqaring.'
                          : 'Foydalanuvchi, moderatsiya va to‘lov qaydlari.',
              style: const TextStyle(color: Color(0xFF7E8A80), height: 1.4)),
          const SizedBox(height: 16),
          if (isUser)
            const _WarningCard(
                text:
                    'Hozircha takliflarni ko‘rish va murojaat yuborish mumkin. Xarid yoki xo‘jalikni boshqarish uchun tegishli rol kerak.'),
          Wrap(
              spacing: 10,
              runSpacing: 10,
              children: stats
                  .map((item) => SizedBox(
                      width: (MediaQuery.sizeOf(context).width - 42) / 2,
                      child: _MetricCard(
                          title: item.$1, value: item.$2, icon: item.$3)))
                  .toList()),
          const SizedBox(height: 18),
          _SectionHeader(
              title: isInvestor
                  ? 'Investitsiyalarim'
                  : isKeeper
                      ? 'Xo‘jalik qaydlari'
                      : role == 'USER'
                          ? 'Faol takliflar'
                          : 'Boshqaruv bo‘limlari',
              subtitle: isInvestor
                  ? 'Buyurtmalarim'
                  : isKeeper
                      ? '${harvests.length} ta hosil, ${expenses.length} ta xarajat'
                      : 'Mavjud ma’lumotlar'),
          const SizedBox(height: 9),
          if (isInvestor)
            ...investments.take(3).map((item) => _SimpleDataTile(
                title: textValue(item['listingTitle'], 'Buyurtma'),
                subtitle:
                    '${labelStatus(textValue(item['status'], ''))} · ${item['unitsQuantity'] ?? 0} birlik',
                trailing: formatMoney(numberValue(item['amount']))))
          else if (isKeeper)
            ...apiaries.take(3).map((item) => _SimpleDataTile(
                title: textValue(item['name'], 'Asalarixona'),
                subtitle:
                    textValue(item['location'], 'Joylashuv ko‘rsatilmagan'),
                trailing:
                    '${colonies.where((c) => c['apiaryId'] == item['id']).fold<int>(0, (sum, c) => sum + intValue(c['count']))} uya'))
          else if (role == 'USER')
            ...listings.take(3).map((item) => _SimpleDataTile(
                title: textValue(item['title'], 'Taklif'),
                subtitle: textValue(item['location'], ''),
                trailing: formatMoney(numberValue(item['unitPrice']))))
          else
            ...mapList(data['moderation']).take(3).map((item) =>
                _SimpleDataTile(
                    title: textValue(item['title'], 'Taklif'),
                    subtitle: textValue(item['location'], ''),
                    trailing: labelStatus(textValue(item['status'], '')))),
          if (role == 'USER' && listings.isEmpty ||
              isInvestor && investments.isEmpty ||
              isKeeper && apiaries.isEmpty)
            const _EmptyCard(
                icon: Icons.inbox_outlined,
                title: 'Hozircha ma’lumot yo‘q',
                text: 'Yangi qaydlar shu yerda ko‘rinadi.'),
          const SizedBox(height: 12),
          Wrap(spacing: 8, runSpacing: 8, children: [
            if (role == 'USER')
              OutlinedButton.icon(
                  onPressed: () => onNavigate('support'),
                  icon: const Icon(Icons.support_agent_rounded),
                  label: const Text('Murojaat yuborish')),
            if (isInvestor)
              OutlinedButton.icon(
                  onPressed: () => onNavigate('payments'),
                  icon: const Icon(Icons.credit_card_outlined),
                  label: const Text('To‘lovlarim')),
            if (isKeeper)
              OutlinedButton.icon(
                  onPressed: () => onNavigate('sensors'),
                  icon: const Icon(Icons.thermostat_outlined),
                  label: const Text('Sensor qaydlari')),
          ]),
          const SizedBox(height: 8),
        ]);
  }
}

class _MarketplacePage extends StatefulWidget {
  const _MarketplacePage(
      {required this.api,
      required this.items,
      required this.role,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> items;
  final String role;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;

  @override
  State<_MarketplacePage> createState() => _MarketplacePageState();
}

class _MarketplacePageState extends State<_MarketplacePage> {
  String _query = '';
  Timer? _searchTimer;
  int _searchGeneration = 0;
  List<Map<String, dynamic>>? _serverResults;
  bool _searching = false;

  @override
  void dispose() {
    _searchTimer?.cancel();
    super.dispose();
  }

  void _onQueryChanged(String value) {
    _searchTimer?.cancel();
    final generation = ++_searchGeneration;
    final term = value.trim();
    setState(() {
      _query = value;
      _serverResults = null;
      _searching = term.isNotEmpty;
    });
    if (term.isEmpty) return;
    _searchTimer = Timer(const Duration(milliseconds: 300),
        () => _searchOnServer(term, generation));
  }

  Future<void> _searchOnServer(String term, int generation) async {
    try {
      final result = await widget.api.get(
          '/listings?query=${Uri.encodeQueryComponent(term)}&page=0&size=100');
      if (!mounted || generation != _searchGeneration) return;
      setState(() {
        _serverResults = mapList(result['content']);
        _searching = false;
      });
    } catch (_) {
      if (!mounted || generation != _searchGeneration) return;
      setState(() {
        _serverResults = null;
        _searching = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final source = _serverResults ?? widget.items;
    final query = _query.toLowerCase().trim();
    final items = source.where((item) {
      if (item['status'] != 'ACTIVE') return false;
      return '${item['title']} ${item['location']} ${item['description']}'
          .toLowerCase()
          .contains(query);
    }).toList();
    return ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          const _SectionHeader(
              title: 'Takliflar bozori',
              subtitle: 'Faol asalarichilik takliflari'),
          const SizedBox(height: 10),
          TextField(
              decoration: const InputDecoration(
                  prefixIcon: Icon(Icons.search_rounded),
                  hintText: 'Taklif yoki joylashuv qidirish'),
              onChanged: _onQueryChanged),
          if (_searching)
            const Padding(
                padding: EdgeInsets.only(top: 8),
                child: LinearProgressIndicator(minHeight: 2, color: leaf)),
          const SizedBox(height: 12),
          if (widget.role == 'USER')
            const _WarningCard(
                text:
                    'Oddiy foydalanuvchi takliflarni ko‘rishi mumkin. Buyurtma yaratish uchun administrator Investor rolini tayinlaydi.'),
          if (items.isEmpty && _searching)
            const Padding(
                padding: EdgeInsets.symmetric(vertical: 24),
                child: Center(child: CircularProgressIndicator(color: leaf)))
          else if (items.isEmpty)
            const _EmptyCard(
                icon: Icons.search_off_rounded,
                title: 'Faol taklif topilmadi',
                text:
                    'Qidiruv matnini tekshiring yoki qayta urinib ko‘ring.'),
          ...items.map((item) => Padding(
              padding: const EdgeInsets.only(top: 10),
              child:
                  _ListingCard(item: item, onTap: () => _openDetails(item)))),
          const SizedBox(height: 12),
        ]);
  }

  void _openDetails(Map<String, dynamic> item) {
    showModalBottomSheet<void>(
        context: context,
        isScrollControlled: true,
        showDragHandle: true,
        builder: (context) => _ListingDetails(
            item: item,
            isInvestor: widget.role == 'INVESTOR',
            onReserve:
                widget.role == 'INVESTOR' ? () => _reserve(item) : null));
  }

  Future<void> _reserve(Map<String, dynamic> item) async {
    try {
      await widget.api.post('/investments', {
        'listingId': item['id'],
        'unitsQuantity': 1,
        'idempotencyKey': 'mobile-${DateTime.now().microsecondsSinceEpoch}',
      });
      if (mounted) Navigator.of(context).pop();
      widget.onMessage('Buyurtma yaratildi.');
      await widget.onChanged();
    } catch (error) {
      widget.onMessage(error.toString());
    }
  }
}

class _ListingDetails extends StatelessWidget {
  const _ListingDetails(
      {required this.item, required this.isInvestor, required this.onReserve});
  final Map<String, dynamic> item;
  final bool isInvestor;
  final Future<void> Function()? onReserve;

  @override
  Widget build(BuildContext context) {
    return Padding(
        padding: const EdgeInsets.fromLTRB(18, 6, 18, 28),
        child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const _Eyebrow('FAOL TAKLIF'),
              const SizedBox(height: 8),
              Text(textValue(item['title'], 'Asalarichilik taklifi'),
                  style: const TextStyle(
                      color: ink, fontSize: 22, fontWeight: FontWeight.w800)),
              const SizedBox(height: 6),
              Text(textValue(item['location'], ''),
                  style: const TextStyle(color: Color(0xFF77857A))),
              const SizedBox(height: 12),
              Text(textValue(item['description'], 'Tavsif ko‘rsatilmagan.'),
                  style: const TextStyle(height: 1.5)),
              const SizedBox(height: 12),
              Row(children: [
                Expanded(
                    child: _MetricCard(
                        title: 'Narx / birlik',
                        value: formatMoney(numberValue(item['unitPrice'])),
                        icon: Icons.payments_outlined)),
                const SizedBox(width: 9),
                Expanded(
                    child: _MetricCard(
                        title: 'Mavjud',
                        value: '${item['unitsAvailable'] ?? 0}',
                        icon: Icons.hive_outlined))
              ]),
              const SizedBox(height: 14),
              if (isInvestor && onReserve != null) ...[
                const SizedBox(height: 12),
                SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                        onPressed: () => onReserve!(),
                        icon: const Icon(Icons.bookmark_add_outlined),
                        label: const Text('Buyurtma berish'))),
              ],
            ]));
  }
}

class _InvestmentsPage extends StatelessWidget {
  const _InvestmentsPage(
      {required this.api,
      required this.investments,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> investments;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;

  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        const _SectionHeader(
            title: 'Investitsiyalarim',
            subtitle: 'Buyurtma va to‘lov holatlari'),
        const SizedBox(height: 12),
        if (investments.isEmpty)
          const _EmptyCard(
              icon: Icons.account_balance_wallet_outlined,
              title: 'Buyurtma yo‘q',
              text:
                  'Faol listingni tanlab buyurtma yarating.'),
        ...investments.map((item) => Padding(
            padding: const EdgeInsets.only(bottom: 9),
            child: Card(
                child: ListTile(
              leading: const CircleAvatar(
                  backgroundColor: Color(0xFFEAF3EA),
                  child: Icon(Icons.hive_outlined, color: forest)),
              title: Text(textValue(item['listingTitle'], 'Buyurtma'),
                  style: const TextStyle(fontWeight: FontWeight.w700)),
              subtitle: Text(
                  '${labelStatus(textValue(item['status'], ''))} · ${item['unitsQuantity'] ?? 0} birlik\n${textValue(item['location'], '')}'),
              isThreeLine: true,
              trailing: Text(formatMoney(numberValue(item['amount'])),
                  style: const TextStyle(
                      color: forest, fontWeight: FontWeight.w700)),
              onLongPress: item['status'] == 'PENDING_PAYMENT'
                  ? () => _cancel(context, item)
                  : null,
            )))),
      ]);

  Future<void> _cancel(BuildContext context, Map<String, dynamic> item) async {
    try {
      await api.delete('/investments/${item['id']}');
      onMessage('Buyurtma bekor qilindi.');
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }
}

class _PaymentsPage extends StatelessWidget {
  const _PaymentsPage({required this.payments, required this.admin});
  final List<Map<String, dynamic>> payments;
  final bool admin;
  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        _SectionHeader(
            title: admin ? 'To‘lov qaydlari' : 'To‘lovlarim',
            subtitle: admin
                ? 'To‘lov yozuvlari'
                : 'Hisobingizga bog‘langan to‘lov yozuvlari'),
        const SizedBox(height: 12),
        if (payments.isEmpty)
          const _EmptyCard(
              icon: Icons.credit_card_off_outlined,
              title: 'To‘lov qaydi yo‘q',
              text:
                  'Tranzaksiya yozuvlari paydo bo‘lgach shu yerda ko‘rinadi.'),
        ...payments.map((item) => _SimpleDataTile(
            title: textValue(item['provider'], 'To‘lov usuli'),
            subtitle:
                '${labelStatus(textValue(item['status'], ''))} · ${textValue(item['createdAt'], '')}',
            trailing: formatMoney(numberValue(item['amount'])))),
        const SizedBox(height: 12),
      ]);
}

class _ApiariesPage extends StatelessWidget {
  const _ApiariesPage(
      {required this.api,
      required this.apiaries,
      required this.colonies,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> apiaries;
  final List<Map<String, dynamic>> colonies;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;

  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        _SectionHeader(
            title: 'Asalarixonalar',
            subtitle: '${apiaries.length} ta asalarixona',
            action: IconButton.filledTonal(
                onPressed: () => _edit(context),
                icon: const Icon(Icons.add_rounded))),
        const SizedBox(height: 12),
        if (apiaries.isEmpty)
          const _EmptyCard(
              icon: Icons.hive_outlined,
              title: 'Asalarixona yo‘q',
              text:
                  'Avval xo‘jalikni yarating, keyin ari oilasi va hisobotlarni qo‘shing.'),
        ...apiaries.map((apiary) {
          final related = colonies
              .where((colony) => colony['apiaryId'] == apiary['id'])
              .toList();
          return Padding(
              padding: const EdgeInsets.only(bottom: 9),
              child: Card(
                  child: ExpansionTile(
                leading: const CircleAvatar(
                    backgroundColor: Color(0xFFEAF3EA),
                    child: Icon(Icons.hive_outlined, color: forest)),
                title: Text(textValue(apiary['name'], 'Asalarixona'),
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                subtitle: Text(textValue(apiary['location'], 'Joylashuv yo‘q')),
                childrenPadding: const EdgeInsets.fromLTRB(14, 0, 14, 12),
                children: [
                  Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                          textValue(apiary['description'], 'Tavsif yo‘q'),
                          style: const TextStyle(color: Color(0xFF7C887E)))),
                  Align(
                      alignment: Alignment.centerLeft,
                      child: Text('${related.length} ta ari oilasi yozuvi',
                          style: const TextStyle(
                              color: Color(0xFF7C887E), fontSize: 12))),
                  ...related.map((colony) => ListTile(
                        dense: true,
                        leading: const Icon(Icons.grid_view_rounded),
                        title: Text(textValue(colony['code'], 'Ari oilasi')),
                        subtitle: Text(
                            '${colony['count'] ?? 0} uya · ${labelStatus(textValue(colony['status'], ''))}'),
                        trailing: PopupMenuButton<String>(
                          onSelected: (action) => action == 'edit'
                              ? _editColony(context, apiary, colony)
                              : _deactivateColony(context, colony),
                          itemBuilder: (context) => const [
                            PopupMenuItem(
                                value: 'edit', child: Text('Tahrirlash')),
                            PopupMenuItem(
                                value: 'off', child: Text('Faol emas')),
                          ],
                        ),
                      )),
                  Wrap(spacing: 8, children: [
                    TextButton.icon(
                        onPressed: () => _edit(context, apiary),
                        icon: const Icon(Icons.edit_outlined),
                        label: const Text('Tahrirlash')),
                    TextButton.icon(
                        onPressed: () => _addColony(context, apiary),
                        icon: const Icon(Icons.add),
                        label: const Text('Ari oilasi')),
                    if (apiary['status'] == 'ACTIVE')
                      TextButton.icon(
                          onPressed: () => _deactivateApiary(context, apiary),
                          icon: const Icon(Icons.pause_circle_outline),
                          label: const Text('Faol emas'))
                  ]),
                ],
              )));
        }),
      ]);

  Future<void> _edit(BuildContext context,
      [Map<String, dynamic>? apiary]) async {
    final result = await showDialog<Map<String, String>>(
        context: context,
        builder: (_) => _TextFormDialog(
                title: apiary == null
                    ? 'Asalarixona qo‘shish'
                    : 'Asalarixonani tahrirlash',
                fields: {
                  'name': textValue(apiary?['name'], ''),
                  'location': textValue(apiary?['location'], ''),
                  'description': textValue(apiary?['description'], '')
                },
                labels: const {
                  'name': 'Nomi',
                  'location': 'Joylashuv',
                  'description': 'Tavsif'
                }));
    if (result == null) return;
    try {
      final body = {
        'name': result['name']!.trim(),
        'location': result['location']!.trim(),
        'description': result['description']!.trim()
      };
      if (apiary == null) {
        await api.post('/apiaries', body);
      } else {
        await api.put('/apiaries/${apiary['id']}', body);
      }
      onMessage(apiary == null
          ? 'Asalarixona yaratildi.'
          : 'Asalarixona yangilandi.');
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }

  Future<void> _addColony(BuildContext context, Map<String, dynamic> apiary) =>
      _editColony(context, apiary, null);

  Future<void> _editColony(BuildContext context, Map<String, dynamic> apiary,
      Map<String, dynamic>? colony) async {
    final result = await showDialog<Map<String, String>>(
        context: context,
        builder: (_) => _TextFormDialog(
                title: colony == null
                    ? 'Ari oilasi qo‘shish'
                    : 'Ari oilasini tahrirlash',
                fields: {
                  'code': textValue(colony?['code'], ''),
                  'count': textValue(colony?['count'], '1'),
                  'strength': textValue(colony?['strength'], ''),
                  'description': textValue(colony?['description'], '')
                },
                labels: const {
                  'code': 'Oila kodi',
                  'count': 'Uyalar soni',
                  'strength': 'Kuchi (WEAK/MEDIUM/STRONG)',
                  'description': 'Izoh'
                }));
    if (result == null) return;
    try {
      final body = {
        'code': result['code']!.trim(),
        'count': int.tryParse(result['count']!) ?? 1,
        'strength': result['strength']!.trim(),
        'description': result['description']!.trim()
      };
      if (colony == null) {
        await api.post('/colonies?apiaryId=${apiary['id']}', body);
      } else {
        await api.put('/colonies/${colony['id']}', body);
      }
      onMessage(
          colony == null ? 'Ari oilasi qo‘shildi.' : 'Ari oilasi yangilandi.');
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }

  Future<void> _deactivateApiary(
      BuildContext context, Map<String, dynamic> apiary) async {
    if (!await confirmAction(
        context, 'Asalarixonani faol bo‘lmagan holatga o‘tkazasizmi?')) {
      return;
    }
    try {
      await api.delete('/apiaries/${apiary['id']}');
      onMessage('Asalarixona faol bo‘lmagan holatga o‘tkazildi.');
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }

  Future<void> _deactivateColony(
      BuildContext context, Map<String, dynamic> colony) async {
    if (!await confirmAction(
        context, 'Ari oilasini faol bo‘lmagan holatga o‘tkazasizmi?')) {
      return;
    }
    try {
      await api.delete('/colonies/${colony['id']}');
      onMessage('Ari oilasi faol bo‘lmagan holatga o‘tkazildi.');
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }
}

class _HarvestsPage extends StatefulWidget {
  const _HarvestsPage(
      {required this.api,
      required this.apiaries,
      required this.harvests,
      required this.expenses,
      required this.admin,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> apiaries;
  final List<Map<String, dynamic>> harvests;
  final List<Map<String, dynamic>> expenses;
  final bool admin;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;
  @override
  State<_HarvestsPage> createState() => _HarvestsPageState();
}

class _HarvestsPageState extends State<_HarvestsPage> {
  bool _showExpenses = false;
  @override
  Widget build(BuildContext context) {
    final items = _showExpenses ? widget.expenses : widget.harvests;
    final pending =
        widget.harvests.where((item) => item['status'] == 'SUBMITTED').toList();
    return ListView(padding: const EdgeInsets.all(16), children: [
      _SectionHeader(
          title: widget.admin ? 'Hosil moderatsiyasi' : 'Hosil va xarajatlar',
          subtitle: widget.admin
              ? '${pending.length} ta ko‘rib chiqilmagan hisobot'
              : 'Xo‘jalik qaydlari',
          action: widget.admin
              ? null
              : PopupMenuButton<String>(
                  onSelected: (value) => _create(context, value),
                  itemBuilder: (_) => const [
                        PopupMenuItem(
                            value: 'harvest',
                            child: Text('Hosil hisobotini qo‘shish')),
                        PopupMenuItem(
                            value: 'expense', child: Text('Xarajat qo‘shish'))
                      ],
                  child: const Icon(Icons.add_circle_outline,
                      size: 28, color: forest))),
      if (!widget.admin)
        Padding(
            padding: const EdgeInsets.only(top: 10),
            child: SegmentedButton<bool>(
                segments: const [
                  ButtonSegment(value: false, label: Text('Hosil')),
                  ButtonSegment(value: true, label: Text('Xarajat'))
                ],
                selected: {
                  _showExpenses
                },
                onSelectionChanged: (value) =>
                    setState(() => _showExpenses = value.first))),
      const SizedBox(height: 11),
      if (widget.admin && pending.isNotEmpty)
        ...pending.map((item) => Card(
            child: ListTile(
                title: Text(
                    '${item['quantityKg'] ?? 0} kg · ${apiaryName(widget.apiaries, item['apiaryId'])}'),
                subtitle: Text(
                    '${item['harvestedOn'] ?? ''} · ${textValue(item['note'], '')}'),
                trailing: PopupMenuButton<String>(
                    onSelected: (status) => _review(item, status),
                    itemBuilder: (_) => const [
                          PopupMenuItem(
                              value: 'APPROVED', child: Text('Tasdiqlash')),
                          PopupMenuItem(
                              value: 'RETURNED',
                              child: Text('Tuzatishga qaytarish'))
                        ])))),
      if (items.isEmpty)
        _EmptyCard(
            icon: _showExpenses
                ? Icons.receipt_long_outlined
                : Icons.water_drop_outlined,
            title: _showExpenses ? 'Xarajat qaydi yo‘q' : 'Hosil qaydi yo‘q',
            text: widget.admin
                ? 'Ko‘rib chiqiladigan hisobot topilmadi.'
                : 'Yangi qaydni + menyusidan qo‘shing.'),
      ...items.map((item) => _SimpleDataTile(
          title: _showExpenses
              ? formatMoney(numberValue(item['amount']))
              : '${item['quantityKg'] ?? 0} kg',
          subtitle:
              '${apiaryName(widget.apiaries, item['apiaryId'])} · ${item[_showExpenses ? 'spentOn' : 'harvestedOn'] ?? ''}',
          trailing: labelStatus(
              textValue(item['status'], textValue(item['category'], 'Qayd'))))),
      if (!widget.admin)
        const _WarningCard(
            text:
                'Hosil va xarajatlar asalarichi tomonidan yuborilgan qaydlar. Admin tekshiruvi talab qilinishi mumkin.'),
    ]);
  }

  Future<void> _create(BuildContext context, String kind) async {
    if (widget.apiaries.isEmpty) {
      widget.onMessage('Avval asalarixona yarating.');
      return;
    }
    final result = await showDialog<Map<String, String>>(
        context: context,
        builder: (_) => _RecordDialog(
            title: kind == 'harvest' ? 'Hosil hisoboti' : 'Xarajat qaydi',
            apiaries: widget.apiaries,
            harvest: kind == 'harvest'));
    if (result == null) return;
    try {
      final apiaryId = result['apiaryId']!;
      if (kind == 'harvest') {
        await widget.api.post('/harvests', {
          'apiaryId': apiaryId,
          'quantityKg': double.tryParse(result['amount']!) ?? 0,
          'harvestedOn': result['date'],
          'note': result['note']
        });
        widget.onMessage('Hosil hisoboti yuborildi.');
      } else {
        await widget.api.post('/expenses', {
          'apiaryId': apiaryId,
          'category': result['category'],
          'amount': double.tryParse(result['amount']!) ?? 0,
          'spentOn': result['date'],
          'note': result['note']
        });
        widget.onMessage('Xarajat qaydi saqlandi.');
      }
      await widget.onChanged();
    } catch (error) {
      widget.onMessage(error.toString());
    }
  }

  Future<void> _review(Map<String, dynamic> item, String status) async {
    try {
      await widget.api
          .patch('/harvests/${item['id']}/review', {'status': status});
      widget.onMessage(status == 'APPROVED'
          ? 'Hisobot tasdiqlandi.'
          : 'Hisobot qaytarildi.');
      await widget.onChanged();
    } catch (error) {
      widget.onMessage(error.toString());
    }
  }
}

class _SensorsPage extends StatelessWidget {
  const _SensorsPage(
      {required this.api,
      required this.apiaries,
      required this.colonies,
      required this.readings,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> apiaries;
  final List<Map<String, dynamic>> colonies;
  final List<Map<String, dynamic>> readings;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;

  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        _SectionHeader(
            title: 'Sensor qaydlari',
            subtitle: 'Qo‘lda kiritilgan, MANUAL manbali o‘lchovlar',
            action: IconButton.filledTonal(
                onPressed: () => _create(context),
                icon: const Icon(Icons.add_rounded))),
        const SizedBox(height: 8),
        const _WarningCard(
            text:
                'Haqiqiy IoT qurilmasi ulanmagan. Bu bo‘limdagi o‘lchovlar faqat qo‘lda kiritilgan qaydlardir.'),
        if (apiaries.isEmpty)
          const _EmptyCard(
              icon: Icons.hive_outlined,
              title: 'Asalarixona kerak',
              text: 'Sensor yozuvi asalarixonaga bog‘lanadi.'),
        if (readings.isEmpty)
          const _EmptyCard(
              icon: Icons.thermostat_outlined,
              title: 'Sensor qaydi yo‘q',
              text: 'O‘lchovni qo‘lda kiritish uchun + tugmasini bosing.'),
        ...readings.map((item) => _SimpleDataTile(
            title:
                '${textValue(item['metric'], '')} · ${item['value'] ?? ''} ${textValue(item['unit'], '')}',
            subtitle:
                '${apiaryName(apiaries, item['apiaryId'])} · ${textValue(item['recordedAt'], '')}',
            trailing: textValue(item['source'], 'MANUAL'))),
      ]);

  Future<void> _create(BuildContext context) async {
    if (apiaries.isEmpty) {
      onMessage('Avval asalarixona yarating.');
      return;
    }
    final result = await showDialog<Map<String, String>>(
        context: context,
        builder: (_) => _SensorDialog(apiaries: apiaries, colonies: colonies));
    if (result == null) return;
    final metric = result['metric']!;
    final unit = metric == 'TEMPERATURE'
        ? 'C'
        : metric == 'HUMIDITY'
            ? '%'
            : 'kg';
    try {
      await api.post('/sensor-readings', {
        'apiaryId': result['apiaryId'],
        'colonyId':
            result['colonyId']?.isEmpty == true ? null : result['colonyId'],
        'metric': metric,
        'value': double.tryParse(result['value']!) ?? 0,
        'unit': unit,
        'source': 'MANUAL',
        'recordedAt': DateTime.now().toUtc().toIso8601String()
      });
      onMessage('MANUAL sensor qaydi saqlandi.');
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }
}

class _ModerationPage extends StatelessWidget {
  const _ModerationPage(
      {required this.api,
      required this.items,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> items;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;
  @override
  Widget build(BuildContext context) {
    final queue = items
        .where((item) =>
            item['status'] == 'MODERATION' || item['status'] == 'DRAFT')
        .toList();
    return ListView(padding: const EdgeInsets.all(16), children: [
      _SectionHeader(
          title: 'Takliflar moderatsiyasi',
          subtitle: '${queue.length} ta tekshiruv kutilmoqda'),
      if (queue.isEmpty)
        const _EmptyCard(
            icon: Icons.verified_outlined,
            title: 'Navbat bo‘sh',
            text: 'Ko‘rib chiqilishi kerak bo‘lgan listing yo‘q.'),
      ...queue.map((item) => Card(
          child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(textValue(item['title'], 'Taklif'),
                        style: const TextStyle(
                            fontSize: 16, fontWeight: FontWeight.w800)),
                    const SizedBox(height: 5),
                    Text(
                        '${textValue(item['location'], '')} · ${formatMoney(numberValue(item['unitPrice']))}',
                        style: const TextStyle(color: Color(0xFF728074))),
                    const SizedBox(height: 7),
                    Text(textValue(item['description'], ''),
                        maxLines: 4, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 10),
                    Row(children: [
                      Expanded(
                          child: FilledButton.icon(
                              onPressed: () => _update(item, 'ACTIVE'),
                              icon: const Icon(Icons.check_rounded),
                              label: const Text('Tasdiqlash'))),
                      const SizedBox(width: 8),
                      Expanded(
                          child: OutlinedButton(
                              onPressed: () => _update(item, 'DRAFT'),
                              child: const Text('Qaytarish')))
                    ]),
                  ])))),
    ]);
  }

  Future<void> _update(Map<String, dynamic> item, String status) async {
    try {
      await api.patch(
          '/listings/${item['id']}/status', {'status': status, 'note': ''});
      onMessage(status == 'ACTIVE'
          ? 'Taklif tasdiqlandi.'
          : 'Taklif qoralamaga qaytarildi.');
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }
}

class _UsersPage extends StatelessWidget {
  const _UsersPage(
      {required this.api,
      required this.users,
      required this.canAssignAdmin,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> users;
  final bool canAssignAdmin;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;

  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        _SectionHeader(
            title: 'Foydalanuvchilar',
            subtitle: '${users.length} ta foydalanuvchi'),
        const _WarningCard(
            text:
                'Admin rolini faqat Super Admin tayinlay oladi. Vakolat cheklovlari har bir amal bajarilishida tekshiriladi.'),
        if (users.isEmpty)
          const _EmptyCard(
              icon: Icons.people_outline_rounded,
              title: 'Foydalanuvchi topilmadi',
              text: 'Foydalanuvchilar mavjud bo‘lgach, shu yerda ko‘rinadi.'),
        ...users.map((user) => Card(
                child: ListTile(
              leading: CircleAvatar(
                  backgroundColor: const Color(0xFFEAF3EA),
                  child: Text(
                      initials(textValue(
                          user['fullName'], textValue(user['phone'], 'U'))),
                      style: const TextStyle(
                          color: forest, fontWeight: FontWeight.w700))),
              title: Text(textValue(user['fullName'], 'Ism kiritilmagan'),
                  style: const TextStyle(fontWeight: FontWeight.w700)),
              subtitle: Text(
                  '${textValue(user['phone'], '')}\n${mapList(user['roles']).isEmpty ? (user['roles'] as List? ?? []).join(', ') : (user['roles'] as List? ?? []).join(', ')} · ${labelStatus(textValue(user['accountStatus'], ''))}'),
              isThreeLine: true,
              trailing: IconButton(
                  tooltip: 'Rollarni boshqarish',
                  onPressed: () => _editRoles(context, user),
                  icon: const Icon(Icons.manage_accounts_outlined)),
            ))),
      ]);

  Future<void> _editRoles(
      BuildContext context, Map<String, dynamic> user) async {
    final result = await showDialog<List<String>>(
        context: context,
        builder: (_) =>
            _RoleDialog(user: user, canAssignAdmin: canAssignAdmin));
    if (result == null) return;
    try {
      await api.patch('/admin/users/${user['id']}/roles', {'roles': result});
      onMessage('Foydalanuvchi rollari yangilandi.');
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }
}

class _RoleDialog extends StatefulWidget {
  const _RoleDialog({required this.user, required this.canAssignAdmin});
  final Map<String, dynamic> user;
  final bool canAssignAdmin;
  @override
  State<_RoleDialog> createState() => _RoleDialogState();
}

class _RoleDialogState extends State<_RoleDialog> {
  late Set<String> _selected;
  final _options = const [
    'USER',
    'INVESTOR',
    'BEEKEEPER',
    'ADMIN',
    'SUPER_ADMIN'
  ];
  @override
  void initState() {
    super.initState();
    _selected = ((widget.user['roles'] as List?) ?? const [])
        .map((e) => e.toString())
        .toSet();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
          title: const Text('Foydalanuvchi rollari'),
          content: SingleChildScrollView(
              child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: _options
                      .map((role) => CheckboxListTile(
                          value: _selected.contains(role),
                          title: Text(role),
                          dense: true,
                          onChanged: (value) {
                            if ((role == 'ADMIN' || role == 'SUPER_ADMIN') &&
                                !widget.canAssignAdmin) {
                              return;
                            }
                            setState(() {
                              value == true
                                  ? _selected.add(role)
                                  : _selected.remove(role);
                            });
                          }))
                      .toList())),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Bekor qilish')),
            FilledButton(
                onPressed: _selected.isEmpty
                    ? null
                    : () => Navigator.pop(context, _selected.toList()),
                child: const Text('Saqlash'))
          ]);
}

class _SupportPage extends StatefulWidget {
  const _SupportPage(
      {required this.api,
      required this.tickets,
      required this.admin,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> tickets;
  final bool admin;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;
  @override
  State<_SupportPage> createState() => _SupportPageState();
}

class _SupportPageState extends State<_SupportPage> {
  String _category = 'GENERAL';
  final _subject = TextEditingController();
  final _message = TextEditingController();
  @override
  void dispose() {
    _subject.dispose();
    _message.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        _SectionHeader(
            title: widget.admin
                ? 'Foydalanuvchi murojaatlari'
                : 'Yordam va murojaatlar',
            subtitle: widget.admin
                ? '${widget.tickets.length} ta murojaat'
                : 'Savol, texnik muammo yoki shikoyat yuboring.'),
        if (!widget.admin)
          Card(
              child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(children: [
                    DropdownButtonFormField<String>(
                        initialValue: _category,
                        decoration:
                            const InputDecoration(labelText: 'Murojaat turi'),
                        items: const [
                          DropdownMenuItem(
                              value: 'GENERAL', child: Text('Umumiy savol')),
                          DropdownMenuItem(
                              value: 'TECHNICAL', child: Text('Texnik muammo')),
                          DropdownMenuItem(
                              value: 'COMPLAINT', child: Text('Shikoyat'))
                        ],
                        onChanged: (value) =>
                            setState(() => _category = value ?? 'GENERAL')),
                    const SizedBox(height: 10),
                    TextField(
                        controller: _subject,
                        maxLength: 180,
                        decoration: const InputDecoration(labelText: 'Mavzu')),
                    const SizedBox(height: 8),
                    TextField(
                        controller: _message,
                        maxLength: 4000,
                        maxLines: 4,
                        decoration: const InputDecoration(
                            labelText: 'Batafsil ma’lumot')),
                    const SizedBox(height: 8),
                    SizedBox(
                        width: double.infinity,
                        child: FilledButton.icon(
                            onPressed: _submit,
                            icon: const Icon(Icons.send_rounded),
                            label: const Text('Murojaat yuborish'))),
                    const SizedBox(height: 6),
                    const Text(
                        'Parol, SMS kodi yoki karta ma’lumotini xabarga kiritmang.',
                        style:
                            TextStyle(color: Color(0xFF87948A), fontSize: 11)),
                  ]))),
        const SizedBox(height: 12),
        if (widget.tickets.isEmpty)
          _EmptyCard(
              icon: Icons.support_agent_rounded,
              title: 'Murojaat yo‘q',
              text: widget.admin
                  ? 'Yangi murojaat kelmagan.'
                  : 'Siz yuborgan murojaatlar shu yerda ko‘rinadi.'),
        ...widget.tickets.map((ticket) => Card(
                child: ExpansionTile(
                    title: Text(textValue(ticket['subject'], 'Murojaat'),
                        style: const TextStyle(fontWeight: FontWeight.w700)),
                    subtitle: Text(
                        '${supportStatus(textValue(ticket['status'], 'OPEN'))} · ${textValue(ticket['category'], '')}'),
                    children: [
                  ListTile(
                      title: Text(textValue(ticket['message'], '')),
                      subtitle: Text(textValue(ticket['adminNote'], ''))),
                  if (widget.admin)
                    Wrap(
                      spacing: 4,
                      children:
                          ['IN_REVIEW', 'RESOLVED', 'CLOSED'].map((status) {
                        return TextButton(
                          onPressed: () => _review(ticket, status),
                          child: Text(supportStatus(status)),
                        );
                      }).toList(),
                    )
                ]))),
      ]);

  Future<void> _submit() async {
    if (_subject.text.trim().isEmpty || _message.text.trim().isEmpty) {
      widget.onMessage('Mavzu va murojaat matnini kiriting.');
      return;
    }
    try {
      await widget.api.post('/support/tickets', {
        'category': _category,
        'subject': _subject.text.trim(),
        'message': _message.text.trim()
      });
      _subject.clear();
      _message.clear();
      widget.onMessage('Murojaat yuborildi.');
      await widget.onChanged();
    } catch (error) {
      widget.onMessage(error.toString());
    }
  }

  Future<void> _review(Map<String, dynamic> ticket, String status) async {
    try {
      await widget.api.patch('/admin/support/tickets/${ticket['id']}/status',
          {'status': status, 'adminNote': textValue(ticket['adminNote'], '')});
      widget.onMessage('Murojaat yangilandi.');
      await widget.onChanged();
    } catch (error) {
      widget.onMessage(error.toString());
    }
  }
}

class _NotificationsPage extends StatelessWidget {
  const _NotificationsPage(
      {required this.api,
      required this.notifications,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final List<Map<String, dynamic>> notifications;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;
  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        _SectionHeader(
            title: 'Bildirishnomalar',
            subtitle: '${notifications.length} ta bildirishnoma',
            action: TextButton(
                onPressed: _markAll, child: const Text('Barchasini o‘qildi'))),
        if (notifications.isEmpty)
          const _EmptyCard(
              icon: Icons.notifications_none_rounded,
              title: 'Bildirishnoma yo‘q',
              text: 'Yangi xabarlar shu yerda ko‘rinadi.'),
        ...notifications.map((item) => Card(
            child: ListTile(
                leading: Icon(
                    item['readAt'] == null
                        ? Icons.mark_email_unread_outlined
                        : Icons.drafts_outlined,
                    color: item['readAt'] == null ? forest : Colors.grey),
                title: Text(textValue(item['title'], 'Bildirishnoma'),
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                subtitle: Text(
                    '${textValue(item['body'], '')}\n${textValue(item['createdAt'], '')}'),
                isThreeLine: true,
                onTap: () => item['readAt'] == null ? _mark(item) : null))),
      ]);
  Future<void> _mark(Map<String, dynamic> item) async {
    try {
      await api.patch('/notifications/${item['id']}/read', {});
      await onChanged();
    } catch (error) {
      onMessage(error.toString());
    }
  }

  Future<void> _markAll() async {
    try {
      await api.patch('/notifications/read-all', {});
      await onChanged();
      onMessage('Bildirishnomalar o‘qildi deb belgilandi.');
    } catch (error) {
      onMessage(error.toString());
    }
  }
}

class _SettingsPage extends StatelessWidget {
  const _SettingsPage(
      {required this.api,
      required this.preferences,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final Map<String, dynamic> preferences;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;
  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        const _SectionHeader(
            title: 'Bildirishnoma sozlamalari',
            subtitle: 'O‘zingizga kerakli yangilanishlarni tanlang.'),
        ...[
          ('investmentUpdates', 'Buyurtma yangiliklari'),
          ('reportUpdates', 'Hosil va xarajat xabarlari'),
          ('marketingUpdates', 'Platforma e’lonlari'),
          ('pushEnabled', 'Push bildirishnomalar'),
        ].map((item) => SwitchListTile(
            title: Text(item.$2),
            value: preferences[item.$1] == true,
            onChanged: (value) => _save(item.$1, value),
            contentPadding: EdgeInsets.zero)),
      ]);
  Future<void> _save(String key, bool value) async {
    final body = {
      'investmentUpdates': preferences['investmentUpdates'] == true,
      'reportUpdates': preferences['reportUpdates'] == true,
      'marketingUpdates': preferences['marketingUpdates'] == true,
      'pushEnabled': preferences['pushEnabled'] == true
    };
    body[key] = value;
    try {
      await api.patch('/notifications/preferences', body);
      await onChanged();
      onMessage('Bildirishnoma sozlamalari saqlandi.');
    } catch (error) {
      onMessage(error.toString());
    }
  }
}

class _ReportsPage extends StatelessWidget {
  const _ReportsPage(
      {required this.role, required this.report, required this.data});
  final String role;
  final Map<String, dynamic> report;
  final Map<String, dynamic> data;
  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        const _SectionHeader(
            title: 'Hisobotlar',
            subtitle: 'Jamlanma ma’lumotlar'),
        _SimpleDataTile(
            title: role == 'INVESTOR'
                ? 'Faol buyurtmalar qiymati'
                : 'Jami hosil',
            subtitle: role == 'INVESTOR'
                ? 'Faol investitsiya yozuvlari'
                : 'Yuborilgan hosil hisobotlari',
            trailing: role == 'INVESTOR'
                ? formatMoney(numberValue(report['activePrincipalAmount']))
                : '${numberValue(report['harvestKg']).toStringAsFixed(1)} kg'),
        if (role == 'INVESTOR')
          _SimpleDataTile(
              title: 'Kutilayotgan buyurtma',
              subtitle: 'To‘lov tasdiqlanmagan',
              trailing: '${report['pendingOrders'] ?? 0} ta')
        else
          _SimpleDataTile(
              title: 'Xarajatlar',
              subtitle: 'Qayd etilgan jami',
              trailing: formatMoney(
                  numberValue(asMap(data['beekeeperReport'])['expensesUzs']))),
      ]);
}

class _AuditPage extends StatelessWidget {
  const _AuditPage({required this.rows});
  final List<Map<String, dynamic>> rows;
  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        const _SectionHeader(
            title: 'Audit jurnali',
            subtitle: 'Administrator amallari tarixi'),
        if (rows.isEmpty)
          const _EmptyCard(
              icon: Icons.receipt_long_outlined,
              title: 'Audit yozuvi yo‘q',
              text: 'Yangi admin harakatlari shu yerda ko‘rinadi.'),
        ...rows.map((item) => _SimpleDataTile(
            title: textValue(item['action'], 'Amal'),
            subtitle:
                '${textValue(item['entityType'], '')} · ${textValue(item['outcome'], '')}',
            trailing: textValue(item['createdAt'], '')))
      ]);
}

class _ProfilePage extends StatefulWidget {
  const _ProfilePage(
      {required this.api,
      required this.profile,
      required this.role,
      required this.onLogout,
      required this.onChanged,
      required this.onMessage});
  final ApiClient api;
  final Map<String, dynamic> profile;
  final String role;
  final Future<void> Function() onLogout;
  final Future<void> Function() onChanged;
  final void Function(String) onMessage;
  @override
  State<_ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<_ProfilePage> {
  late final TextEditingController _name =
      TextEditingController(text: textValue(widget.profile['fullName'], ''));
  late final TextEditingController _email =
      TextEditingController(text: textValue(widget.profile['email'], ''));
  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const _SectionHeader(
              title: 'Profil',
              subtitle: 'Hisob ma’lumotlari'),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  Builder(builder: (context) {
                    ImageProvider<Object>? avatarImage;
                    final imageData = widget.profile['profileImageData'];
                    if (imageData is String &&
                        imageData.startsWith('data:image/jpeg;base64,')) {
                      try {
                        avatarImage = MemoryImage(
                            base64Decode(imageData.substring(imageData.indexOf(',') + 1)));
                      } catch (_) {
                        avatarImage = null;
                      }
                    }
                    return CircleAvatar(
                      radius: 36,
                      backgroundColor: const Color(0xFFEAF3EA),
                      backgroundImage: avatarImage,
                      child: avatarImage == null
                          ? const Icon(Icons.person_outline_rounded,
                              color: forest, size: 32)
                          : null,
                    );
                  }),
                  const SizedBox(height: 10),
                  Text(displayName(widget.profile),
                      style: const TextStyle(
                          color: ink,
                          fontWeight: FontWeight.w800,
                          fontSize: 17)),
                  Text(roleLabel(widget.role),
                      style: const TextStyle(
                          color: leaf, fontWeight: FontWeight.w700)),
                  const SizedBox(height: 14),
                  TextField(
                      controller: _name,
                      decoration:
                          const InputDecoration(labelText: 'Ism familiya')),
                  const SizedBox(height: 9),
                  TextField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(labelText: 'Email')),
                  const SizedBox(height: 12),
                  SizedBox(
                      width: double.infinity,
                      child: OutlinedButton(
                          onPressed: _save,
                          child: const Text('Profilni saqlash'))),
                  const Divider(height: 22),
                  _SimpleDataTile(
                      title: 'Telefon',
                      subtitle: 'OTP orqali tasdiqlanadi',
                      trailing: textValue(widget.profile['phone'], '—')),
                  _SimpleDataTile(
                      title: 'Hisob holati',
                      subtitle: 'Hisob holati',
                      trailing: labelStatus(
                          textValue(widget.profile['accountStatus'], ''))),
                  _SimpleDataTile(
                      title: 'Rollar',
                      subtitle: 'Administrator beradi',
                      trailing: ((widget.profile['roles'] as List?) ?? [])
                          .join(', ')),
                  SizedBox(
                      width: double.infinity,
                      child: TextButton.icon(
                          onPressed: widget.onLogout,
                          icon: const Icon(Icons.logout_rounded),
                          label: const Text('Chiqish'))),
                ],
              ),
            ),
          ),
        ],
      );
  Future<void> _save() async {
    try {
      await widget.api.patch('/users/me',
          {'fullName': _name.text.trim(), 'email': _email.text.trim()});
      await widget.onChanged();
      widget.onMessage('Profil yangilandi.');
    } catch (error) {
      widget.onMessage(error.toString());
    }
  }
}

class _TextFormDialog extends StatefulWidget {
  const _TextFormDialog(
      {required this.title, required this.fields, required this.labels});
  final String title;
  final Map<String, String> fields;
  final Map<String, String> labels;
  @override
  State<_TextFormDialog> createState() => _TextFormDialogState();
}

class _TextFormDialogState extends State<_TextFormDialog> {
  late final Map<String, TextEditingController> _controllers = {
    for (final entry in widget.fields.entries)
      entry.key: TextEditingController(text: entry.value)
  };
  @override
  void dispose() {
    for (final controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: Text(widget.title),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: widget.fields.keys.map((key) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 9),
                child: TextField(
                  controller: _controllers[key],
                  maxLines: key == 'description' ? 3 : 1,
                  keyboardType: key == 'count'
                      ? TextInputType.number
                      : TextInputType.text,
                  decoration: InputDecoration(labelText: widget.labels[key]),
                ),
              );
            }).toList(),
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Bekor qilish')),
          FilledButton(
            onPressed: () => Navigator.pop(context, {
              for (final entry in _controllers.entries)
                entry.key: entry.value.text
            }),
            child: const Text('Saqlash'),
          ),
        ],
      );
}

class _RecordDialog extends StatefulWidget {
  const _RecordDialog(
      {required this.title, required this.apiaries, required this.harvest});
  final String title;
  final List<Map<String, dynamic>> apiaries;
  final bool harvest;
  @override
  State<_RecordDialog> createState() => _RecordDialogState();
}

class _RecordDialogState extends State<_RecordDialog> {
  late String _apiaryId = textValue(widget.apiaries.first['id'], '');
  String _category = 'FEED';
  final _amount = TextEditingController();
  final _note = TextEditingController();
  @override
  void dispose() {
    _amount.dispose();
    _note.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
          title: Text(widget.title),
          content: SingleChildScrollView(
              child: Column(mainAxisSize: MainAxisSize.min, children: [
            DropdownButtonFormField<String>(
                initialValue: _apiaryId,
                decoration: const InputDecoration(labelText: 'Asalarixona'),
                items: widget.apiaries
                    .map((item) => DropdownMenuItem(
                        value: textValue(item['id'], ''),
                        child: Text(textValue(item['name'], 'Asalarixona'))))
                    .toList(),
                onChanged: (value) =>
                    setState(() => _apiaryId = value ?? _apiaryId)),
            const SizedBox(height: 8),
            if (!widget.harvest)
              DropdownButtonFormField<String>(
                  initialValue: _category,
                  decoration: const InputDecoration(labelText: 'Xarajat turi'),
                  items: const [
                    DropdownMenuItem(value: 'FEED', child: Text('Ozuqa')),
                    DropdownMenuItem(
                        value: 'VETERINARY', child: Text('Veterinariya')),
                    DropdownMenuItem(value: 'EQUIPMENT', child: Text('Uskuna')),
                    DropdownMenuItem(
                        value: 'TRANSPORT', child: Text('Transport')),
                    DropdownMenuItem(value: 'OTHER', child: Text('Boshqa'))
                  ],
                  onChanged: (value) =>
                      setState(() => _category = value ?? 'OTHER')),
            const SizedBox(height: 8),
            TextField(
                controller: _amount,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                    labelText: widget.harvest ? 'Miqdor (kg)' : 'Summa (UZS)')),
            const SizedBox(height: 8),
            TextField(
                controller: _note,
                maxLines: 3,
                decoration: const InputDecoration(labelText: 'Izoh')),
          ])),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Bekor qilish')),
            FilledButton(
                onPressed: () => Navigator.pop(context, {
                      'apiaryId': _apiaryId,
                      'amount': _amount.text,
                      'note': _note.text,
                      'date': DateTime.now().toIso8601String().substring(0, 10),
                      'category': _category
                    }),
                child: const Text('Yuborish'))
          ]);
}

class _SensorDialog extends StatefulWidget {
  const _SensorDialog({required this.apiaries, required this.colonies});
  final List<Map<String, dynamic>> apiaries;
  final List<Map<String, dynamic>> colonies;
  @override
  State<_SensorDialog> createState() => _SensorDialogState();
}

class _SensorDialogState extends State<_SensorDialog> {
  late String _apiaryId = textValue(widget.apiaries.first['id'], '');
  String _metric = 'TEMPERATURE';
  String? _colony;
  final _value = TextEditingController();
  @override
  void dispose() {
    _value.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colonies =
        widget.colonies.where((item) => item['apiaryId'] == _apiaryId).toList();
    return AlertDialog(
        title: const Text('Qo‘lda sensor o‘lchovi'),
        content: SingleChildScrollView(
            child: Column(mainAxisSize: MainAxisSize.min, children: [
          DropdownButtonFormField<String>(
              initialValue: _apiaryId,
              decoration: const InputDecoration(labelText: 'Asalarixona'),
              items: widget.apiaries
                  .map((item) => DropdownMenuItem(
                      value: textValue(item['id'], ''),
                      child: Text(textValue(item['name'], ''))))
                  .toList(),
              onChanged: (value) => setState(() {
                    _apiaryId = value ?? _apiaryId;
                    _colony = null;
                  })),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
              initialValue: _metric,
              decoration: const InputDecoration(labelText: 'Ko‘rsatkich'),
              items: const [
                DropdownMenuItem(
                    value: 'TEMPERATURE', child: Text('Harorat (°C)')),
                DropdownMenuItem(value: 'HUMIDITY', child: Text('Namlik (%)')),
                DropdownMenuItem(value: 'WEIGHT', child: Text('Uya vazni (kg)'))
              ],
              onChanged: (value) =>
                  setState(() => _metric = value ?? 'TEMPERATURE')),
          const SizedBox(height: 8),
          if (colonies.isNotEmpty)
            DropdownButtonFormField<String?>(
                initialValue: _colony,
                decoration:
                    const InputDecoration(labelText: 'Ari oilasi (ixtiyoriy)'),
                items: [
                  const DropdownMenuItem<String?>(
                      value: null, child: Text('Tanlanmagan')),
                  ...colonies.map((item) => DropdownMenuItem<String?>(
                      value: textValue(item['id'], ''),
                      child: Text(textValue(item['code'], 'Ari oilasi'))))
                ],
                onChanged: (value) => setState(() => _colony = value)),
          const SizedBox(height: 8),
          TextField(
              controller: _value,
              keyboardType: const TextInputType.numberWithOptions(
                  decimal: true, signed: true),
              decoration: InputDecoration(
                  labelText: _metric == 'TEMPERATURE'
                      ? 'Harorat (°C)'
                      : _metric == 'HUMIDITY'
                          ? 'Namlik (%)'
                          : 'Vazn (kg)')),
        ])),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Bekor qilish')),
          FilledButton(
              onPressed: () => Navigator.pop(context, {
                    'apiaryId': _apiaryId,
                    'colonyId': _colony ?? '',
                    'metric': _metric,
                    'value': _value.text
                  }),
              child: const Text('Saqlash'))
        ]);
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader(
      {required this.title, required this.subtitle, this.action});
  final String title;
  final String subtitle;
  final Widget? action;
  @override
  Widget build(BuildContext context) =>
      Row(crossAxisAlignment: CrossAxisAlignment.center, children: [
        Expanded(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title,
              style: const TextStyle(
                  color: ink, fontSize: 19, fontWeight: FontWeight.w800)),
          const SizedBox(height: 4),
          Text(subtitle,
              style: const TextStyle(color: Color(0xFF839087), fontSize: 12))
        ])),
        if (action != null) action!
      ]);
}

class _MetricCard extends StatelessWidget {
  const _MetricCard(
      {required this.title, required this.value, required this.icon});
  final String title;
  final String value;
  final IconData icon;
  @override
  Widget build(BuildContext context) => Card(
      child: Padding(
          padding: const EdgeInsets.all(13),
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Container(
                width: 33,
                height: 33,
                decoration: BoxDecoration(
                    color: const Color(0xFFEAF3EA),
                    borderRadius: BorderRadius.circular(10)),
                child: Icon(icon, color: forest, size: 18)),
            const SizedBox(height: 11),
            Text(title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: Color(0xFF839087), fontSize: 11)),
            const SizedBox(height: 4),
            Text(value,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                    color: ink, fontWeight: FontWeight.w800, fontSize: 16))
          ])));
}

class _SimpleDataTile extends StatelessWidget {
  const _SimpleDataTile(
      {required this.title, required this.subtitle, required this.trailing});
  final String title;
  final String subtitle;
  final String trailing;
  @override
  Widget build(BuildContext context) => Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
          title: Text(title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style:
                  const TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
          subtitle:
              Text(subtitle, maxLines: 2, overflow: TextOverflow.ellipsis),
          trailing: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 110),
              child: Text(trailing,
                  textAlign: TextAlign.end,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                      color: forest,
                      fontWeight: FontWeight.w700,
                      fontSize: 11)))));
}

class _ListingCard extends StatelessWidget {
  const _ListingCard({required this.item, required this.onTap});
  final Map<String, dynamic> item;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Card(
      child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.location_on_outlined,
                            color: leaf, size: 16),
                        const SizedBox(width: 4),
                        Expanded(
                            child: Text(
                                textValue(item['location'],
                                    'Joylashuv ko‘rsatilmagan'),
                                style: const TextStyle(
                                    color: Color(0xFF839087), fontSize: 12))),
                        _StatusBadge(
                            label: labelStatus(
                                textValue(item['status'], 'ACTIVE')))
                      ],
                    ),
                    const SizedBox(height: 7),
                    Text(textValue(item['title'], 'Asalarichilik taklifi'),
                        style: const TextStyle(
                            color: ink,
                            fontWeight: FontWeight.w800,
                            fontSize: 16)),
                    const SizedBox(height: 6),
                    Text(textValue(item['description'], ''),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                            color: Color(0xFF748176), height: 1.4)),
                    const SizedBox(height: 10),
                    Row(children: [
                      Expanded(
                          child: Text(
                              formatMoney(numberValue(item['unitPrice'])),
                              style: const TextStyle(
                                  color: forest, fontWeight: FontWeight.w800))),
                      Text('${item['unitsAvailable'] ?? 0} birlik',
                          style: const TextStyle(
                              color: Color(0xFF839087), fontSize: 12)),
                      const SizedBox(width: 6),
                      const Icon(Icons.chevron_right_rounded,
                          color: Color(0xFF839087))
                    ]),
                  ]))));
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.label});
  final String label;
  @override
  Widget build(BuildContext context) => Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
          color: const Color(0xFFEAF3EA),
          borderRadius: BorderRadius.circular(20)),
      child: Text(label,
          style: const TextStyle(
              color: forest, fontSize: 10, fontWeight: FontWeight.w700)));
}

class _WarningCard extends StatelessWidget {
  const _WarningCard({required this.text});
  final String text;
  @override
  Widget build(BuildContext context) => Container(
      margin: const EdgeInsets.symmetric(vertical: 7),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
          color: const Color(0xFFFFF7E8),
          border: Border.all(color: const Color(0xFFF1E5CC)),
          borderRadius: BorderRadius.circular(12)),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Icon(Icons.info_outline_rounded,
            color: Color(0xFF9A7633), size: 18),
        const SizedBox(width: 8),
        Expanded(
            child: Text(text,
                style: const TextStyle(
                    color: Color(0xFF78663E), fontSize: 12, height: 1.4)))
      ]));
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});
  final String message;
  @override
  Widget build(BuildContext context) => Container(
      padding: const EdgeInsets.all(11),
      decoration: BoxDecoration(
          color: const Color(0xFFFFF0ED),
          borderRadius: BorderRadius.circular(10)),
      child: Row(children: [
        const Icon(Icons.error_outline_rounded,
            color: Color(0xFFB25342), size: 18),
        const SizedBox(width: 8),
        Expanded(
            child: Text(message,
                style: const TextStyle(color: Color(0xFF843F33), fontSize: 12)))
      ]));
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard(
      {required this.icon, required this.title, required this.text});
  final IconData icon;
  final String title;
  final String text;
  @override
  Widget build(BuildContext context) => Card(
      child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 23),
          child: Column(children: [
            Icon(icon, color: leaf, size: 27),
            const SizedBox(height: 8),
            Text(title,
                textAlign: TextAlign.center,
                style:
                    const TextStyle(color: ink, fontWeight: FontWeight.w800)),
            const SizedBox(height: 5),
            Text(text,
                textAlign: TextAlign.center,
                style: const TextStyle(
                    color: Color(0xFF839087), fontSize: 12, height: 1.45))
          ])));
}

class _FaqItem extends StatelessWidget {
  const _FaqItem({required this.question, required this.answer, this.onTap});
  final String question;
  final String answer;
  final VoidCallback? onTap;
  @override
  Widget build(BuildContext context) => ExpansionTile(
          title: Text(question,
              style:
                  const TextStyle(fontWeight: FontWeight.w700, fontSize: 13)),
          children: [
            Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(answer,
                          style: const TextStyle(
                              color: Color(0xFF728074),
                              fontSize: 12,
                              height: 1.5)),
                      if (onTap != null)
                        TextButton(
                            onPressed: onTap,
                            child: const Text('Kirish va murojaat yuborish'))
                    ]))
          ]);
}

class _Eyebrow extends StatelessWidget {
  const _Eyebrow(this.text);
  final String text;
  @override
  Widget build(BuildContext context) => Text(text,
      style: const TextStyle(
          color: Color(0xFF9E7A32),
          letterSpacing: 1.1,
          fontSize: 10,
          fontWeight: FontWeight.w800));
}

class _BrandTitle extends StatelessWidget {
  const _BrandTitle({this.label = 'MELARIUM'});
  final String label;
  @override
  Widget build(BuildContext context) => Row(children: [
        Container(
            width: 31,
            height: 31,
            decoration: BoxDecoration(
                color: const Color(0xFFF2BF62),
                borderRadius: BorderRadius.circular(10)),
            child: const Icon(Icons.eco_rounded, color: forest, size: 18)),
        const SizedBox(width: 8),
        Text(label)
      ]);
}

class _ButtonProgress extends StatelessWidget {
  const _ButtonProgress();
  @override
  Widget build(BuildContext context) => const SizedBox(
      width: 18,
      height: 18,
      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white));
}

class _PageSpec {
  const _PageSpec(this.id, this.title, this.icon);
  final String id;
  final String title;
  final IconData icon;
}

List<Map<String, dynamic>> mapList(dynamic value) {
  if (value is! List) return <Map<String, dynamic>>[];
  return value
      .whereType<Map>()
      .map((item) => Map<String, dynamic>.from(item))
      .toList();
}

Map<String, dynamic> asMap(dynamic value) =>
    value is Map ? Map<String, dynamic>.from(value) : <String, dynamic>{};
String textValue(dynamic value, [String fallback = '']) =>
    value == null ? fallback : value.toString();
int intValue(dynamic value) =>
    value is num ? value.toInt() : int.tryParse(value?.toString() ?? '') ?? 0;
double numberValue(dynamic value) => value is num
    ? value.toDouble()
    : double.tryParse(value?.toString() ?? '') ?? 0;
String formatMoney(double amount) {
  final digits = amount.round().abs().toString();
  final grouped = StringBuffer();
  for (var index = 0; index < digits.length; index++) {
    if (index > 0 && (digits.length - index) % 3 == 0) grouped.write(' ');
    grouped.write(digits[index]);
  }
  final sign = amount < 0 ? '-' : '';
  return '$sign${grouped.toString()} so‘m';
}

String displayName(Map<String, dynamic> profile) =>
    textValue(profile['fullName']).trim().isEmpty
        ? textValue(profile['phone'], 'Foydalanuvchi')
        : textValue(profile['fullName']).trim();
String initials(String value) => value
    .trim()
    .split(RegExp(r'\s+'))
    .take(2)
    .map((part) => part.isEmpty ? '' : part[0].toUpperCase())
    .join();
String roleLabel(String role) {
  const labels = <String, String>{
    'USER': 'Oddiy foydalanuvchi',
    'INVESTOR': 'Investor',
    'BEEKEEPER': 'Asalarichi',
    'SUPER_ADMIN': 'Super Admin',
    'ADMIN': 'Administrator',
  };
  return labels[role] ?? 'Foydalanuvchi';
}

String labelStatus(String value) {
  const labels = <String, String>{
    'ACTIVE': 'Faol',
    'MODERATION': 'Moderatsiyada',
    'DRAFT': 'Qoralama',
    'PENDING_PAYMENT': 'To‘lov kutilmoqda',
    'PAYMENT_REVIEW': 'Qo‘lda tekshiruv',
    'CANCELLED': 'Bekor qilingan',
    'APPROVED': 'Tasdiqlangan',
    'SUBMITTED': 'Tekshiruvda',
    'RETURNED': 'Tuzatishga qaytarildi',
    'INACTIVE': 'Faol emas',
    'SUCCESS': 'Tasdiqlangan',
    'FAILED': 'Muvaffaqiyatsiz',
    'OPEN': 'Ochiq',
    'IN_REVIEW': 'Ko‘rib chiqilmoqda',
    'RESOLVED': 'Yechilgan',
    'CLOSED': 'Yopilgan',
  };
  return labels[value] ?? value;
}

String supportStatus(String value) {
  const labels = <String, String>{
    'OPEN': 'Ochiq',
    'IN_REVIEW': 'Ko‘rib chiqilmoqda',
    'RESOLVED': 'Yechilgan',
    'CLOSED': 'Yopilgan',
  };
  return labels[value] ?? value;
}

String apiaryName(List<Map<String, dynamic>> apiaries, dynamic apiaryId) =>
    textValue(
        apiaries.where((item) => item['id'] == apiaryId).firstOrNull?['name'],
        'Asalarixona');
Future<bool> confirmAction(BuildContext context, String message) async =>
    await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(content: Text(message), actions: [
              TextButton(
                  onPressed: () => Navigator.pop(context, false),
                  child: const Text('Bekor qilish')),
              FilledButton(
                  onPressed: () => Navigator.pop(context, true),
                  child: const Text('Tasdiqlash'))
            ])) ??
    false;
