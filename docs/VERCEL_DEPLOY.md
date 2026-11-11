# Vercel va backend environment sozlamalari

## Joriy deploy holati

Hozirgi repository sozlamasida Vercel frontend uchun (`vercel.json`), `.github/workflows/deploy.yml` esa Spring Boot backend JAR’ini serverga joylash uchun ishlaydi; amaldagi backend deploy tafsilotlari `docs/DEPLOY.md` da. Demak, Vercel frontend loyihasiga qo‘shilgan server secret’lari alohida serverdagi backend’ga avtomatik o‘tmaydi.

Quyidagi auth qiymatlarini backend haqiqatan ishlayotgan muhitga kiriting. Agar kelajakda backend Vercel Container Runtime’ga ko‘chirilsa, ularni Vercel Project → **Settings → Environment Variables** bo‘limida server-side o‘zgaruvchilar sifatida sozlang. `VITE_` prefiksini ishlatmang: bunday prefiks frontend build’ida brauzerga ochilishi mumkin.

## Bosqichma-bosqich sozlash

1. **Admin juftliklarini belgilang.** Loyiha egasi nazoratidagi email va O‘zbekiston telefon raqamlarini tanlang. Super Admin juftligi Admin juftligidan alohida bo‘lsin. Backend emailni `trim` va kichik harfga o‘tkazadi; telefon `+998XXXXXXXXX` formatiga normallanadi.
2. **PostgreSQL ma’lumotlarini oling.** PostgreSQL provayderining project/database sahifasidagi `Connect` yoki connection details oynasidan host, port, database nomi, username va password’ni oling. `DATABASE_URL` JDBC formatida bo‘lishi kerak: `jdbc:postgresql://HOST:PORT/DATABASE` (TLS talabi bo‘lsa provayder ko‘rsatgan SSL parametrlarini qo‘shing). Alohida username/password qiymatlarini ham backend muhitiga kiriting.
3. **SMS provayderini tayyorlang.** Tanlangan SMS gateway’da loyiha/account oching, API URL va tokenni oling, `MELARIUM` sender ID’ni provayderda tasdiqlating. Backend `Authorization: Bearer ...` header va `to`, `from`, `text` kalitlariga ega JSON body’ni kutadi; provider API boshqacha bo‘lsa, adapter moslashtirilmaguncha production OTP yuborilmaydi.
4. **Kriptografik qiymatlarni yarating.** Ishonchli lokal terminalda quyidagini ikki marta, alohida bajaring; natijalar bir-biridan farq qilishi kerak:

   ```bash
   openssl rand -base64 32
   openssl rand -base64 32
   ```

   Birinchi natijani `JWT_SECRET_BASE64`, ikkinchisini `OTP_PEPPER_BASE64` uchun ishlating. Qiymatlarni chatga yubormang va Git’ga commit qilmang.
5. **Environment o‘zgaruvchilarini kiriting.** Quyidagi server-side variable’larni production backend’ga qo‘shing:

   ```text
   DATABASE_URL
   DATABASE_USERNAME
   DATABASE_PASSWORD
   JWT_SECRET_BASE64
   OTP_PEPPER_BASE64
   SMS_PROVIDER
   SMS_API_URL
   SMS_API_TOKEN
   SMS_SENDER_ID
   APP_SECURITY_ADMIN_EMAIL
   APP_SECURITY_ADMIN_PHONE
   APP_SECURITY_SUPER_ADMIN_EMAIL
   APP_SECURITY_SUPER_ADMIN_PHONE
   AUTH_EXPOSE_OTP=false
   ```

   Joriy backend alohida serverda ishlayotgan bo‘lsa, avval serverda `sudo systemctl cat melarium` bilan service’ning `Environment` yoki `EnvironmentFile` manbasini toping; shu manbaga variable’larni server egasi xavfsiz qo‘shsin. Vercel Container Runtime’da ishlatishga ko‘chirilgan bo‘lsa, Vercel Project → **Settings → Environment Variables** da Production’ni tanlab qo‘shing. Preview uchun alohida qiymatlar kerak bo‘lsa, Preview’ni ham tanlang. So‘ng backend’ni redeploy/restart qiling.
6. **Deploy va tekshiruv.** Deploy logida Flyway V7 xatosiz o‘tganini, backend health endpoint `/api/v1/health` javob berishini tekshiring. Avval oddiy test juftligi `USER` bo‘lishini, so‘ng faqat serverda sozlangan Admin va Super Admin juftliklari tegishli rollarni olishini tasdiqlang. Production’da juftliklardan biri to‘liq sozlanmagan bo‘lsa, backend ataylab ishga tushmaydi; joriy deploy workflow health check’dan o‘tkazib, oldingi service versiyasiga qaytarishi mumkin.

## Auth variable’lari qanday ishlaydi

- `APP_SECURITY_ADMIN_EMAIL` + `APP_SECURITY_ADMIN_PHONE` aynan mos bo‘lsa `ADMIN` beriladi.
- `APP_SECURITY_SUPER_ADMIN_EMAIL` + `APP_SECURITY_SUPER_ADMIN_PHONE` aynan mos bo‘lsa `ADMIN` va `SUPER_ADMIN` beriladi.
- Boshqa yangi juftliklar `USER` oladi. Mijozdan rol qabul qilinmaydi.
- Har bir email/telefon juftligi to‘liq bo‘lishi shart. Production startup ikkala privileged juftlikni ham talab qiladi; `APP_SECURITY_BOOTSTRAP_ADMIN_PHONE` eski phone-only usuli endi ishlatilmaydi.
- Eski foydalanuvchilarda email `NULL` bo‘lishi mumkin: ular telefon OTP’sini tasdiqlab, emailni kiritgach backend emailni saqlaydi. Case-insensitive email uniqueness migration’i oldindan takrorlangan qiymatlarni normallashtiradi, eng eski hisobda saqlaydi va qolgan takrorlarni `NULL` qiladi.

## Vercel plan va runtime eslatmasi

Vercel Container Runtime ishlatilsa, uning Docker/Container konfiguratsiyasi va amaldagi billing’ini deploydan oldin tekshiring; container ishlatishni bepul yoki cheksiz deb hisoblamang. Hobby plan shaxsiy, notijorat foydalanish shartlariga ega; marketplace tijoriy faoliyat bo‘lsa, tegishli plan va Vercel shartlariga mosligini tekshiring.

Rasmiy qo‘llanmalar: [Vercel Container Runtime](https://vercel.com/docs/functions/runtimes/container), [Hobby plan](https://vercel.com/docs/plans/hobby), [Git deploy](https://vercel.com/docs/git).
