import 'package:flutter_test/flutter_test.dart';
import 'package:melarium_mobile/main.dart';

void main() {
  test('investor status labels are localized and money is formatted', () {
    expect(labelStatus('PENDING_PAYMENT'), 'To‘lov kutilmoqda');
    expect(formatMoney(1234567), '1 234 567 so‘m');
  });

  test('ordinary account is presented separately from investor role', () {
    expect(roleLabel('USER'), 'Oddiy foydalanuvchi');
    expect(roleLabel('INVESTOR'), 'Investor');
  });
}
