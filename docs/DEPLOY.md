# Avtomatik deploy

`main` branch'ga har bir push avtomatik ravishda production'ga chiqadi:

- **Frontend** — Vercel (`https://melarium-cyan.vercel.app`). Sozlamalar: root `vercel.json`. `/api/*` so‘rovlari backend serverga proxy qilinadi.
- **Backend** — Oracle Cloud VM, `.github/workflows/deploy.yml` orqali. Workflow JAR'ni build qiladi, serverga yuklaydi va `melarium` service'ni qayta ishga tushiradi. Health check (`/api/v1/health`) o‘tmasa, oldingi versiya avtomatik qaytariladi.

## O‘zgartirish kiritishda qoidalar

- `vercel.json` va `.github/workflows/deploy.yml` fayllarini o‘chirmang va API proxy manzilini o‘zgartirmang.
- Mavjud Flyway migratsiyalarini (`backend/src/main/resources/db/migration/V1__…`–`V5__…`) **tahrirlamang**. Schema o‘zgarishi uchun yangi fayl qo‘shing: `V6__tavsif.sql`, `V7__…` va hokazo. Aks holda server ishga tushmaydi.
- Hibernate `validate` rejimida ishlaydi: entity'ga yangi ustun qo‘shilsa, unga mos migratsiya ham bo‘lishi shart.
- Maxfiy qiymatlar (DB paroli, JWT secret va h.k.) serverda saqlanadi. Ularni repo'ga commit qilmang.
- Server kichik (1 GB RAM): og‘ir kutubxonalar qo‘shishdan saqlaning.

Deploy holatini GitHub'dagi **Actions → Deploy backend** bo‘limida ko‘rish mumkin.
