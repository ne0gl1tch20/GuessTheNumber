import 'package:flutter_test/flutter_test.dart';
import 'package:gtn_flutter/main.dart';

void main() {
  testWidgets('migration scaffold starts without errors', (tester) async {
    await tester.pumpWidget(const GtnApp());

    expect(find.text('Guess The Number'), findsOneWidget);
    expect(find.text('Flutter migration workspace'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
