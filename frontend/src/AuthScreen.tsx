import { useState, type FormEvent } from 'react';
import { ArrowLeft, ArrowRight, CircleHelp, Clock3, Flower2, Hexagon, Leaf, LoaderCircle, ShieldCheck, Smartphone } from 'lucide-react';
import { api, saveSession, type UserProfile } from './api';

type Props = { onAuthenticated: (user: UserProfile) => void; onBack?: () => void };

function normalizeUzPhone(value: string) {
  const digits = value.replace(/\D/g, '');
  const local = digits.startsWith('998') ? digits.slice(3) : digits;
  return `+998${local}`;
}

export default function AuthScreen({ onAuthenticated, onBack }: Props) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [challenge, setChallenge] = useState<{ deliveryMode: string; debugCode?: string | null; expiresInSeconds: number } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submitPhone = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    const normalized = normalizeUzPhone(phone);
    if (!/^\+998\d{9}$/.test(normalized)) {
      setError('O‘zbekiston telefon raqamini 9 ta raqam bilan kiriting.');
      return;
    }
    setBusy(true);
    try {
      const result = await api.requestOtp(normalized);
      setPhone(normalized);
      setChallenge(result);
      setCode('');
    } catch (issue) {
      setError(issue instanceof Error ? issue.message : 'OTP yuborilmadi. Qayta urinib ko‘ring.');
    } finally {
      setBusy(false);
    }
  };

  const submitCode = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    if (!/^\d{6}$/.test(code)) { setError('SMS kod 6 ta raqamdan iborat bo‘lishi kerak.'); return; }
    setBusy(true);
    try {
      const tokens = await api.verifyOtp(phone, code);
      saveSession(tokens);
      const user = await api.me();
      onAuthenticated(user);
    } catch (issue) {
      setError(issue instanceof Error ? issue.message : 'Kod tasdiqlanmadi. Qayta urinib ko‘ring.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-shell">
      <div className="auth-decoration auth-decoration-one" />
      <div className="auth-decoration auth-decoration-two" />
      <header className="auth-topline"><div className="auth-brand"><span className="auth-brand-mark"><Hexagon size={29} /><Leaf size={13} fill="currentColor" /></span><span><strong>MELARIUM</strong><small>ASALARICHILIK EKOTIZIMI</small></span></div><div className="auth-topline-actions">{onBack && <button type="button" onClick={onBack}><ArrowLeft size={14} /> Ochiq sahifaga qaytish</button>}<a href="mailto:support@melarium.uz"><CircleHelp size={15} /> Yordam</a></div></header>
      <section className="auth-card">
        <div className="auth-visual"><div className="auth-visual-image" /><div className="auth-visual-shade" /><div className="auth-visual-content"><span className="hero-badge"><Flower2 size={13} /> TABIAT BILAN HAMNAFAS</span><h1>Asalarichilik<br />kelajagiga birga.</h1><p>Xo‘jalik faoliyati va hisobotlarni yagona platformada kuzating.</p><div className="auth-visual-foot"><span><ShieldCheck size={14} /> Xavfsiz OTP kirish</span><span>·</span><span>Web · Android · iOS</span></div></div></div>
        <div className="auth-form-wrap">
          {!challenge ? (
            <form className="auth-form" onSubmit={submitPhone}>
              <div className="auth-step-icon"><Smartphone size={20} /></div>
              <span className="auth-eyebrow">XUSH KELIBSIZ</span>
              <h2>Hisobingizga kiring</h2>
              <p>Telefon raqamingizga bir martalik tasdiqlash kodini yuboramiz.</p>
              <label htmlFor="auth-phone">Telefon raqam</label>
              <div className="auth-phone-field"><span>+998</span><input id="auth-phone" type="tel" inputMode="numeric" autoComplete="tel-national" placeholder="90 123 45 67" value={phone.replace(/^\+?998/, '')} onChange={(event) => setPhone(event.target.value.replace(/^\+?998/, ''))} aria-describedby="auth-phone-hint" /></div>
              <small id="auth-phone-hint" className="auth-hint">O‘zbekiston raqami, masalan 90 123 45 67</small>
              {error && <div className="auth-error" role="alert">{error}</div>}
              <button className="button button-primary auth-submit" type="submit" disabled={busy}>{busy ? <LoaderCircle size={16} className="spin" /> : null} Kod yuborish <ArrowRight size={16} /></button>
              <div className="auth-legal">Davom etish orqali platformadan foydalanish qoidalariga rozilik bildirasiz. Investitsiya shartlari va moliyaviy jarayonlar alohida tasdiqlanmaguncha to‘lov olinmaydi.</div>
            </form>
          ) : (
            <form className="auth-form" onSubmit={submitCode}>
              <button className="auth-back" type="button" onClick={() => { setChallenge(null); setCode(''); setError(''); }}><ArrowLeft size={15} /> Raqamni o‘zgartirish</button>
              <div className="auth-step-icon"><ShieldCheck size={20} /></div>
              <span className="auth-eyebrow">TELEFONNI TASDIQLASH</span>
              <h2>Kodni kiriting</h2>
              <p><strong>{phone}</strong> raqamiga yuborilgan 6 xonali kodni kiriting.</p>
              <label htmlFor="auth-code">Tasdiqlash kodi</label>
              <input className="auth-code-input" id="auth-code" type="text" inputMode="numeric" autoComplete="one-time-code" maxLength={6} placeholder="••••••" value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))} />
              <div className="auth-code-meta"><span><Clock3 size={13} /> {Math.floor(challenge.expiresInSeconds / 60)} daqiqa amal qiladi</span><button type="button" onClick={() => submitPhone(new Event('submit') as unknown as FormEvent<HTMLFormElement>)} disabled={busy}>Kodni qayta yuborish</button></div>
              {challenge.debugCode && <div className="local-code-note"><span>LOCAL DEMO KODI</span><strong>{challenge.debugCode}</strong><small>Faqat lokal ishlab chiqish muhitida ko‘rsatiladi.</small></div>}
              {error && <div className="auth-error" role="alert">{error}</div>}
              <button className="button button-primary auth-submit" type="submit" disabled={busy}>{busy ? <LoaderCircle size={16} className="spin" /> : null} Tasdiqlash va kirish <ArrowRight size={16} /></button>
              <div className="auth-legal">Kod bir martalik va 5 daqiqa amal qiladi. Uni boshqa birovga bermang.</div>
            </form>
          )}
        </div>
      </section>
      <footer className="auth-footer"><span>© 2026 MELARIUM</span><span>Shaxsiy ma’lumotlar faqat hisob xavfsizligi va xizmat ko‘rsatish uchun ishlatiladi.</span></footer>
    </main>
  );
}
