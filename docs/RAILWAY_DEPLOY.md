# Railway'ga joylash bo‘yicha qo‘llanma

## Deploy arxitekturasi

Root `Dockerfile` React/Vite frontendini build qilib, statik fayllarni Spring Boot JAR ichiga joylaydi. Natijada Railway’dagi bitta web service frontend va `/api/v1` API’ni bir xil HTTPS origin’da beradi; alohida frontend service yoki CORS domeni talab qilinmaydi. PostgreSQL alohida Railway service bo‘ladi. Server `application.yml` orqali Railway bergan `PORT` qiymatini ishlatadi.

## Railway’da sozlash

1. Railway’da yangi project ochib, GitHub’dagi `bilolkxon4866/melarium` repositorysini `main` branch bilan ulang.
2. Project’ga Railway PostgreSQL service qo‘shing. Service nomini `Postgres` deb qo‘ysangiz, quyidagi variable reference’lar o‘zgarishsiz ishlaydi.
3. Web service’ning **Variables** bo‘limiga quyidagilarni kiriting:

```text
DATABASE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DATABASE_USERNAME=${{Postgres.PGUSER}}
DATABASE_PASSWORD=${{Postgres.PGPASSWORD}}
JWT_SECRET_BASE64=<alohida, tasodifiy 32-byte secret, Base64>
OTP_PEPPER_BASE64=<boshqa, alohida 32-byte secret, Base64>
APP_SECURITY_BOOTSTRAP_ADMIN_PHONE=+998XXXXXXXXX
SMS_API_URL=<SMS provider API base URL>
SMS_API_TOKEN=<provider token>
SMS_SENDER_ID=<provider’da tasdiqlangan sender ID>
```

`JWT_SECRET_BASE64` va `OTP_PEPPER_BASE64` uchun **turli** qiymatlar yarating. Lokal terminalda har biri uchun `openssl rand -base64 32` buyrug‘ini alohida ishga tushirib, qiymatlarni faqat Railway Variables’ga kiriting. Ularni Git’ga commit qilmang va chatga yubormang.

Bir xil domain’da frontend va API ishlatilganda `CORS_ALLOWED_ORIGINS` bo‘sh qolishi mumkin. Alohida frontend yoki custom domain ishlatilsa, bu variable’ga `https://...` origin’ni kiriting. `SPRING_PROFILES_ACTIVE=prod` root Dockerfile’da o‘rnatilgan; OTP kodini response’da chiqaradigan local profile ishlatilmaydi.

PostgreSQL service’ni `Postgres` nomidan boshqacha nom bilan yaratgan bo‘lsangiz, variable reference’lardagi `Postgres` qismini o‘sha service nomiga almashtiring. Railway service variable reference formati: `${{ServiceName.VARIABLE_NAME}}`.

## SMS va production cheklovlari

Production profile SMS credentials bo‘lmasa ataylab ishga tushmaydi. `SMS_API_URL` endpoint’i `HttpSmsSender` kutadigan `Authorization: Bearer ...` header va `{"to":"...","from":"...","text":"..."}` JSON body’ni qo‘llashi kerak. Provider boshqa API ishlatsa, SMS adapter’ni moslashtirish zarur. Haqiqiy SMS provider sozlanmaguncha OTP login ishlamaydi; public deployment’da OTP kodini response yoki log orqali oshkor qilish mumkin emas.

Payment gateway hamda investitsiyaning huquqiy va daromad shartlari production uchun tasdiqlanmagan. Shu sababli real to‘lov yoki daromad oqimi yoqilmaydi. Production profile’da `app-demo-mode=false`.

## Build va health check

- Railway project’da root directory’ni `/` holida qoldiring. Root `Dockerfile` avtomatik topiladi.
- Deploy settings’dagi health check path: `/api/v1/health`.
- Birinchi deploy’da Flyway V1–V5 yangi PostgreSQL database’da schema yaratadi.
- Railway domain bergach, UI’ni HTTPS URL orqali ochib, `/api/v1/health` endpoint’ini tekshiring.

## Hozircha bajarilmagan qadam

Bu ish muhitida Railway connector ishlamaydi; Railway CLI, token yoki Railway project ma’lumoti ham sozlanmagan. Shu sababli Docker deploy tayyorgarligini qo‘shish mumkin, ammo Railway project yaratish, secrets kiritish va real deploy/domain’ni tekshirish hali bajarilmadi. Buning uchun Railway account egasi dashboard’da project va secrets’ni sozlashi yoki project-scoped Railway access’ni bu runtime’ga xavfsiz ulashi kerak.

## Rasmiy hujjatlar

- [Railway PostgreSQL](https://docs.railway.com/databases/postgresql)
- [Railway service variable references](https://docs.railway.com/integrations/api/manage-variables)
