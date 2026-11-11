import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import AuthScreen from './AuthScreen';
import { api, clearSession, hasSession, type AdminDashboardDto, type AdminUserDto, type ApiPage, type ApiaryDto, type AuditLogDto, type BeekeeperReportDto, type ColonyDto, type ExpenseDto, type HarvestDto, type InvestmentDto, type InvestorReportDto, type ListingDto, type NotificationDto, type NotificationPreferencesDto, type PaymentDto, type SensorReadingDto, type SupportTicketDto, type UserProfile } from './api';
import {
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  BarChart3,
  Bell,
  BriefcaseBusiness,
  Building2,
  CalendarDays,
  Camera,
  Check,
  CornerDownLeft,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  CircleDollarSign,
  CircleHelp,
  ClipboardList,
  Clock3,
  CreditCard,
  Download,
  Droplets,
  FileText,
  Flower2,
  Globe2,
  HandCoins,
  Hexagon,
  LayoutDashboard,
  Leaf,
  LoaderCircle,
  LogOut,
  MapPin,
  Menu,
  MoreHorizontal,
  Plus,
  Phone,
  Search,
  Settings2,
  ShieldCheck,
  Sprout,
  Thermometer,
  TrendingUp,
  Trash2,
  UserRound,
  Users,
  WalletCards,
  X,
  type LucideIcon,
} from 'lucide-react';

type Role = 'user' | 'investor' | 'beekeeper' | 'admin';
type PageId =
  | 'overview'
  | 'public'
  | 'listings'
  | 'investments'
  | 'reports'
  | 'notifications'
  | 'support'
  | 'profile'
  | 'search'
  | 'apiaries'
  | 'colonies'
  | 'harvests'
  | 'sensors'
  | 'users'
  | 'payments'
  | 'audit'
  | 'settings';
type ModalId = 'details' | 'invest' | 'harvest' | 'expense' | 'apiary' | 'colony' | 'listing' | 'sensor' | null;
type ListingStatus = 'Faol' | 'Moderatsiyada' | 'Sotilgan' | 'Qoralama' | 'Band' | 'Faol emas';

type NavItem = { id: PageId; label: string; icon: LucideIcon };
type Listing = {
  id: string;
  apiaryId: string;
  name: string;
  place: string;
  beekeeper: string;
  price: number;
  units: number;
  unitsTotal?: number;
  category: string;
  status: ListingStatus;
  description: string;
  createdAt?: string;
  featured?: boolean;
};
type Investment = {
  id: string;
  listingName: string;
  place: string;
  date: string;
  amount: number;
  quantity: number;
  status: string;
  progress: number;
};
type HarvestRecord = { id: string; place: string; date: string; amount: number; note: string; status: string };
type ExpenseRecord = { id: string; place: string; date: string; amount: number; category: string };
type NotificationItem = { id: string; title: string; body: string; time: string; kind: 'payment' | 'report' | 'listing' | 'system'; read: boolean };
type Apiary = { id: string; name: string; place: string; description?: string; colonies: number; status: string };
type ModalProps = { kind: ModalId; listing: Listing | null; apiary: Apiary | null; colony: ColonyDto | null; apiaries: Apiary[]; colonies: ColonyDto[]; canInvest: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void; onOpenInvest: () => void };
type SearchResult = { id: string; category: string; title: string; subtitle: string; searchText: string; page: PageId; icon: LucideIcon; listing?: Listing };

const ROLE_LABELS: Record<Role, string> = {
  user: 'Oddiy foydalanuvchi',
  investor: 'Investor',
  beekeeper: 'Asalarichi',
  admin: 'Administrator',
};

const NAV_ITEMS: Record<Role, NavItem[]> = {
  user: [
    { id: 'overview', label: 'Umumiy ko‘rinish', icon: LayoutDashboard },
    { id: 'listings', label: 'Takliflar bozori', icon: Sprout },
    { id: 'notifications', label: 'Bildirishnomalar', icon: Bell },
    { id: 'support', label: 'Yordam va murojaatlar', icon: CircleHelp },
  ],
  investor: [
    { id: 'overview', label: 'Umumiy ko‘rinish', icon: LayoutDashboard },
    { id: 'listings', label: 'Takliflar bozori', icon: Sprout },
    { id: 'investments', label: 'Investitsiyalarim', icon: BriefcaseBusiness },
    { id: 'payments', label: 'To‘lovlarim', icon: CreditCard },
    { id: 'reports', label: 'Hisobotlar', icon: BarChart3 },
    { id: 'notifications', label: 'Bildirishnomalar', icon: Bell },
    { id: 'support', label: 'Yordam va murojaatlar', icon: CircleHelp },
  ],
  beekeeper: [
    { id: 'overview', label: 'Umumiy ko‘rinish', icon: LayoutDashboard },
    { id: 'apiaries', label: 'Asalarixonalar', icon: Building2 },
    { id: 'colonies', label: 'Ari oilalari', icon: Flower2 },
    { id: 'listings', label: 'Mening takliflarim', icon: Sprout },
    { id: 'harvests', label: 'Hosil va xarajatlar', icon: ClipboardList },
    { id: 'sensors', label: 'Qo‘lda sensor qaydi', icon: Thermometer },
    { id: 'reports', label: 'Hisobotlar', icon: BarChart3 },
    { id: 'support', label: 'Yordam va murojaatlar', icon: CircleHelp },
  ],
  admin: [
    { id: 'overview', label: 'Umumiy ko‘rinish', icon: LayoutDashboard },
    { id: 'users', label: 'Foydalanuvchilar', icon: Users },
    { id: 'listings', label: 'Takliflar nazorati', icon: ShieldCheck },
    { id: 'investments', label: 'Investitsiyalar', icon: BriefcaseBusiness },
    { id: 'payments', label: 'To‘lovlar', icon: CreditCard },
    { id: 'harvests', label: 'Hosil moderatsiyasi', icon: ClipboardList },
    { id: 'sensors', label: 'Sensor qaydlari', icon: Thermometer },
    { id: 'reports', label: 'Analitika', icon: BarChart3 },
    { id: 'audit', label: 'Audit jurnali', icon: ClipboardList },
    { id: 'support', label: 'Murojaatlar', icon: CircleHelp },
  ],
};

function formatMoney(amount: number) {
  return `${new Intl.NumberFormat('uz-UZ').format(amount)} so‘m`;
}

function formatShortMoney(amount: number) {
  if (amount >= 1_000_000) return `${(amount / 1_000_000).toLocaleString('uz-UZ', { maximumFractionDigits: 1 })} mln`;
  if (amount >= 1_000) return `${Math.round(amount / 1_000)} ming`;
  return new Intl.NumberFormat('uz-UZ').format(amount);
}

function formatToday() {
  return new Intl.DateTimeFormat('uz-UZ', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date());
}

export default function App() {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);

  useEffect(() => {
    let alive = true;
    if (!hasSession()) {
      setCheckingSession(false);
      return;
    }
    api.me().then((profile) => { if (alive) setUser(profile); })
      .catch(() => { clearSession(); if (alive) setUser(null); })
      .finally(() => { if (alive) setCheckingSession(false); });
    return () => { alive = false; };
  }, []);

  const finishLogin = (profile: UserProfile) => setUser(profile);
  const logout = async () => {
    try { await api.logout(); } finally { setUser(null); }
  };

  if (checkingSession) return <div className="session-loading"><LoaderCircle className="spin" size={22} /><span>Hisob tekshirilmoqda…</span></div>;
  if (!user) return <GuestApp onAuthenticated={finishLogin} />;
  if (!user.roles.length) return <div className="session-loading"><CircleHelp size={22} /><span>Hisobga rol biriktirilmagan. Administratorga murojaat qiling.</span><button className="button button-outline" onClick={() => void logout()}>Chiqish</button></div>;
  return <DashboardApp user={user} onUserUpdate={setUser} onLogout={logout} />;
}

function GuestApp({ onAuthenticated }: { onAuthenticated: (user: UserProfile) => void }) {
  const [showLogin, setShowLogin] = useState(false);
  const [page, setPage] = useState<'home' | 'market'>('home');
  const [listings, setListings] = useState<Listing[]>([]);
  const [listingCount, setListingCount] = useState(0);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Listing | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let alive = true;
    api.listings('', 0, 100).then((response) => {
      if (alive) { setListings(response.content.map(mapListing)); setListingCount(response.totalElements); }
    }).catch(() => { if (alive) setError('Takliflarni yuklab bo‘lmadi. Qayta urinib ko‘ring.'); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, []);
  if (showLogin) return <AuthScreen onAuthenticated={onAuthenticated} onBack={() => setShowLogin(false)} />;
  return <div className="guest-shell"><header className="guest-header"><button className="brand" onClick={() => setPage('home')}><span className="brand-mark"><Hexagon size={31} strokeWidth={1.8} /><span className="brand-mark-dot"><Leaf size={13} fill="currentColor" /></span></span><span className="brand-copy"><strong>MELARIUM</strong><small>ASALARICHILIK EKOTIZIMI</small></span></button><div><button className="button button-outline" onClick={() => setPage('market')}>Takliflarni ko‘rish</button><button className="button button-primary" onClick={() => setShowLogin(true)}>Kirish / ro‘yxatdan o‘tish <ArrowRight size={15} /></button></div></header><main className="guest-main">{error && <div className="load-error-banner"><CircleHelp size={15} />{error}</div>}{loading && <div className="inline-loading"><LoaderCircle size={15} className="spin" /> Faol takliflar yuklanmoqda…</div>}{page === 'home' ? <PublicPage listings={listings} totalListings={listingCount} onBrowse={() => setPage('market')} onDetail={setSelected} onContact={() => setShowLogin(true)} /> : <MarketplacePage listings={listings} query={query} onQueryChange={setQuery} onDetail={setSelected} />}</main><footer className="guest-footer"><span>© 2026 MELARIUM</span><span>Hisob yaratish va xizmatdan foydalanish uchun OTP kirish talab qilinadi.</span><button onClick={() => setShowLogin(true)}>Kirish</button></footer><Modal kind={selected ? 'details' : null} listing={selected} apiary={null} colony={null} apiaries={[]} colonies={[]} canInvest={false} onClose={() => setSelected(null)} onSubmit={(event) => event.preventDefault()} onOpenInvest={() => setShowLogin(true)} /></div>;
}


function viewRole(value: string): Role | null {
  if (value === 'USER') return 'user';
  if (value === 'INVESTOR') return 'investor';
  if (value === 'BEEKEEPER') return 'beekeeper';
  if (value === 'ADMIN' || value === 'SUPER_ADMIN') return 'admin';
  return null;
}
function initialRole(user: UserProfile): Role {
  const available = user.roles.map(viewRole).filter((role): role is Role => role !== null);
  const stored = sessionStorage.getItem('melarium.active-role') as Role | null;
  if (stored && available.includes(stored)) return stored;
  if (available.includes('admin')) return 'admin';
  if (available.includes('beekeeper')) return 'beekeeper';
  if (available.includes('investor')) return 'investor';
  return 'user';
}
function labelStatus(value: string): string {
  const values: Record<string, string> = {
    active: 'Faol', ACTIVE: 'Faol', moderation: 'Moderatsiyada', MODERATION: 'Moderatsiyada',
    draft: 'Qoralama', DRAFT: 'Qoralama', reserved: 'Band', RESERVED: 'Band',
    sold: 'Sotilgan', SOLD: 'Sotilgan', inactive: 'Faol emas', INACTIVE: 'Faol emas',
    PENDING_PAYMENT: 'To‘lov kutilmoqda', PAYMENT_REVIEW: 'Qo‘lda tekshiriladi',
    CANCELLED: 'Bekor qilingan', REFUNDED: 'Qaytarilgan', COMPLETED: 'Yakunlangan',
    APPROVED: 'Tasdiqlangan', SUBMITTED: 'Tekshiruvda', RETURNED: 'Tuzatish kerak',
    SUSPENDED: 'To‘xtatilgan', CLOSED: 'Yopilgan', PENDING: 'Kutilmoqda', SUCCESS: 'Tasdiqlangan', FAILED: 'Muvaffaqiyatsiz',
  };
  return values[value] || value;
}
function mapListing(value: ListingDto): Listing {
  return {
    id: value.id, apiaryId: value.apiaryId, name: value.title, place: value.location, beekeeper: value.beekeeperId, price: Number(value.unitPrice),
    units: value.unitsAvailable, unitsTotal: value.unitsTotal, category: value.category === 'COLONY' ? 'Asalari oilasi' : value.category,
    status: labelStatus(value.status) as ListingStatus, description: value.description, createdAt: value.createdAt,
  };
}
function mapInvestment(value: InvestmentDto): Investment {
  return {
    id: value.id, listingName: value.listingTitle, place: value.location, date: formatDate(value.createdAt),
    amount: Number(value.amount), quantity: value.unitsQuantity, status: labelStatus(value.status), progress: 0,
  };
}
function mapHarvest(value: HarvestDto, apiaries: ApiaryDto[]): HarvestRecord {
  const apiary = apiaries.find((item) => item.id === value.apiaryId);
  return { id: value.id, place: apiary?.name || 'Asalarixona', date: formatDate(value.harvestedOn), amount: Number(value.quantityKg), note: value.note || '—', status: labelStatus(value.status) };
}
function mapExpense(value: ExpenseDto, apiaries: ApiaryDto[]): ExpenseRecord {
  const apiary = apiaries.find((item) => item.id === value.apiaryId);
  const categories: Record<string, string> = { FEED: 'Ozuqa va parvarish', VETERINARY: 'Veterinariya', EQUIPMENT: 'Uskuna', TRANSPORT: 'Transport', OTHER: 'Boshqa xarajat' };
  return { id: value.id, place: apiary?.name || 'Asalarixona', date: formatDate(value.spentOn), amount: Number(value.amount), category: categories[value.category] || value.category };
}
function mapApiary(value: ApiaryDto, colonies = 0): Apiary {
  return { id: value.id, name: value.name, place: value.location, description: value.description, colonies, status: labelStatus(value.status) };
}
function mapNotification(value: NotificationDto): NotificationItem {
  const kind = value.kind === 'PAYMENT' || value.kind === 'INVESTMENT' ? 'payment'
    : value.kind === 'REPORT' ? 'report' : value.kind === 'LISTING' ? 'listing' : 'system';
  return { id: value.id, title: value.title, body: value.body, time: formatDate(value.createdAt), kind, read: Boolean(value.readAt) };
}
function formatDate(value: string) {
  const date = new Date(value.length === 10 ? `${value}T12:00:00` : value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('uz-UZ', { day: 'numeric', month: 'short', year: 'numeric' }).format(date);
}
function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0] || '').join('').toUpperCase() || 'M';
}

function normalizeSearchText(value: string) {
  return value.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').replace(/[’‘ʻʼ`']/g, '').toLocaleLowerCase('uz-UZ').replace(/\s+/g, ' ').trim();
}

function DashboardApp({ user, onUserUpdate, onLogout }: { user: UserProfile; onUserUpdate: (user: UserProfile) => void; onLogout: () => void }) {
  const [role, setRole] = useState<Role>(() => initialRole(user));
  const [activePage, setActivePage] = useState<PageId>('overview');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [modal, setModal] = useState<ModalId>(null);
  const [selectedListing, setSelectedListing] = useState<Listing | null>(null);
  const [selectedApiary, setSelectedApiary] = useState<Apiary | null>(null);
  const [selectedColony, setSelectedColony] = useState<ColonyDto | null>(null);
  const [toast, setToast] = useState('');
  const [listings, setListings] = useState<Listing[]>([]);
  const [publicListings, setPublicListings] = useState<Listing[]>([]);
  const [publicListingCount, setPublicListingCount] = useState(0);
  const [investments, setInvestments] = useState<Investment[]>([]);
  const [harvests, setHarvests] = useState<HarvestRecord[]>([]);
  const [expenses, setExpenses] = useState<ExpenseRecord[]>([]);
  const [apiaries, setApiaries] = useState<Apiary[]>([]);
  const [colonies, setColonies] = useState<ColonyDto[]>([]);
  const [sensorReadings, setSensorReadings] = useState<SensorReadingDto[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [harvestTab, setHarvestTab] = useState<'harvest' | 'expense'>('harvest');
  const [investorReport, setInvestorReport] = useState<InvestorReportDto | null>(null);
  const [beekeeperReport, setBeekeeperReport] = useState<BeekeeperReportDto | null>(null);
  const [adminDashboard, setAdminDashboard] = useState<AdminDashboardDto | null>(null);
  const [adminUsers, setAdminUsers] = useState<AdminUserDto[]>([]);
  const [adminPayments, setAdminPayments] = useState<PaymentDto[]>([]);
  const [myPayments, setMyPayments] = useState<PaymentDto[]>([]);
  const [adminInvestments, setAdminInvestments] = useState<Investment[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLogDto[]>([]);
  const [preferences, setPreferences] = useState<NotificationPreferencesDto | null>(null);
  const [supportTickets, setSupportTickets] = useState<SupportTicketDto[]>([]);
  const [adminSupportTickets, setAdminSupportTickets] = useState<SupportTicketDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [remoteSearchListings, setRemoteSearchListings] = useState<Listing[] | null>(null);
  const [remoteSearchUsers, setRemoteSearchUsers] = useState<AdminUserDto[] | null>(null);
  const [globalSearchLoading, setGlobalSearchLoading] = useState(false);

  const accountName = user.fullName?.trim() || user.phone;
  const availableRoles = [...new Set(user.roles.map(viewRole).filter((item): item is Role => item !== null))];
  const unreadCount = notifications.filter((item) => !item.read).length;
  const totalInvested = investments.filter((item) => item.status === 'Faol').reduce((sum, item) => sum + item.amount, 0);

  const changePage = (page: PageId) => { setActivePage(page); setMobileMenuOpen(false); setSearchOpen(false); if (page === 'profile') setSearchQuery(''); };
  const showToast = (message: string) => { setToast(message); window.setTimeout(() => setToast(''), 3600); };
  const handleRoleChange = (nextRole: Role) => {
    if (!availableRoles.includes(nextRole)) return;
    setRole(nextRole);
    sessionStorage.setItem('melarium.active-role', nextRole);
    setActivePage('overview');
    setMobileMenuOpen(false);
  };

  const refreshData = async () => {
    setLoading(true);
    setLoadError('');
    const errors: string[] = [];
    const attempt = async <T,>(task: Promise<T>, fallback: T): Promise<T> => {
      try { return await task; } catch {
        if (!errors.length) errors.push('Ma’lumotlarni yuklab bo‘lmadi. Qayta urinib ko‘ring.');
        return fallback;
      }
    };
    const emptyPage = <T,>(): ApiPage<T> => ({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0, first: true, last: true });

    const [activePageResult, ownResult, moderationResult] = await Promise.all([
      attempt(api.listings('', 0, 100), emptyPage<ListingDto>()),
      role === 'beekeeper' ? attempt(api.myListings(), [] as ListingDto[]) : Promise.resolve([] as ListingDto[]),
      role === 'admin' ? attempt(api.moderationQueue(0, 100), emptyPage<ListingDto>()) : Promise.resolve(emptyPage<ListingDto>()),
    ]);
    const activeDtos = activePageResult.content;
    setPublicListingCount(activePageResult.totalElements);
    setPublicListings(activeDtos.map(mapListing));
    setListings(role === 'beekeeper' ? ownResult.map(mapListing)
      : role === 'admin' ? [...activeDtos, ...moderationResult.content].filter((item, index, all) => all.findIndex((other) => other.id === item.id) === index).map(mapListing)
      : activeDtos.map(mapListing));

    if (role === 'investor') {
      const [orders, report, notes, prefs, payments, tickets] = await Promise.all([
        attempt(api.investments(), [] as InvestmentDto[]),
        attempt(api.investorReport(), null),
        attempt(api.notifications(0, 100), emptyPage<NotificationDto>()),
        attempt(api.notificationPreferences(), null),
        attempt(api.myPayments(0, 100), emptyPage<PaymentDto>()),
        attempt(api.supportTickets(0, 100), emptyPage<SupportTicketDto>()),
      ]);
      setInvestments(orders.map(mapInvestment));
      setInvestorReport(report);
      setNotifications(notes.content.map(mapNotification));
      setPreferences(prefs);
      setMyPayments(payments.content);
      setSupportTickets(tickets.content); setAdminSupportTickets([]);
      setApiaries([]); setColonies([]); setSensorReadings([]); setHarvests([]); setExpenses([]);
      setBeekeeperReport(null); setAdminDashboard(null); setAdminUsers([]); setAdminPayments([]); setAdminInvestments([]); setAuditLogs([]);
    } else if (role === 'user') {
      const [notes, prefs, tickets] = await Promise.all([
        attempt(api.notifications(0, 100), emptyPage<NotificationDto>()),
        attempt(api.notificationPreferences(), null),
        attempt(api.supportTickets(0, 100), emptyPage<SupportTicketDto>()),
      ]);
      setNotifications(notes.content.map(mapNotification));
      setPreferences(prefs);
      setSupportTickets(tickets.content); setAdminSupportTickets([]);
      setInvestments([]); setMyPayments([]); setInvestorReport(null); setBeekeeperReport(null);
      setApiaries([]); setColonies([]); setSensorReadings([]); setHarvests([]); setExpenses([]);
      setAdminDashboard(null); setAdminUsers([]); setAdminPayments([]); setAdminInvestments([]); setAuditLogs([]);
    } else if (role === 'beekeeper') {
      const [apiaryDtos, harvestDtos, expenseDtos, notes, report, prefs, tickets] = await Promise.all([
        attempt(api.apiaries(), [] as ApiaryDto[]),
        attempt(api.harvests(), [] as HarvestDto[]),
        attempt(api.expenses(), [] as ExpenseDto[]),
        attempt(api.notifications(0, 100), emptyPage<NotificationDto>()),
        attempt(api.beekeeperReport(), null),
        attempt(api.notificationPreferences(), null),
        attempt(api.supportTickets(0, 100), emptyPage<SupportTicketDto>()),
      ]);
      const colonyLists = await Promise.all(apiaryDtos.map((item) => attempt(api.colonies(item.id), [] as ColonyDto[])));
      const sensorPages = await Promise.all(apiaryDtos.map((item) => attempt(api.sensorReadings(item.id), emptyPage<SensorReadingDto>())));
      const allColonies = colonyLists.flat();
      setApiaries(apiaryDtos.map((item) => mapApiary(item, allColonies.filter((colony) => colony.apiaryId === item.id).reduce((sum, colony) => sum + colony.count, 0))));
      setColonies(allColonies);
      setSensorReadings(sensorPages.flatMap((page) => page.content));
      setHarvests(harvestDtos.map((item) => mapHarvest(item, apiaryDtos)));
      setExpenses(expenseDtos.map((item) => mapExpense(item, apiaryDtos)));
      setNotifications(notes.content.map(mapNotification));
      setBeekeeperReport(report);
      setPreferences(prefs);
      setSupportTickets(tickets.content); setAdminSupportTickets([]);
      setInvestments([]); setMyPayments([]); setInvestorReport(null); setAdminDashboard(null); setAdminUsers([]); setAdminPayments([]); setAdminInvestments([]); setAuditLogs([]);
    } else {
      const [stats, users, apiaryDtos, harvestDtos, expenseDtos, notes, payments, adminOrders, logs, prefs, tickets] = await Promise.all([
        attempt(api.adminDashboard(), null),
        attempt(api.adminUsers('', 0, 100), emptyPage<AdminUserDto>()),
        attempt(api.apiaries(), [] as ApiaryDto[]),
        attempt(api.harvests(), [] as HarvestDto[]),
        attempt(api.expenses(), [] as ExpenseDto[]),
        attempt(api.notifications(0, 100), emptyPage<NotificationDto>()),
        attempt(api.adminPayments(0, 100), emptyPage<PaymentDto>()),
        attempt(api.adminInvestments(0, 100), emptyPage<InvestmentDto>()),
        attempt(api.auditLogs(0, 100), emptyPage<AuditLogDto>()),
        attempt(api.notificationPreferences(), null),
        attempt(api.adminSupportTickets('', 0, 100), emptyPage<SupportTicketDto>()),
      ]);
      const colonyLists = await Promise.all(apiaryDtos.map((item) => attempt(api.colonies(item.id), [] as ColonyDto[])));
      const sensorPages = await Promise.all(apiaryDtos.map((item) => attempt(api.sensorReadings(item.id), emptyPage<SensorReadingDto>())));
      const allColonies = colonyLists.flat();
      setAdminDashboard(stats);
      setAdminUsers(users.content);
      setApiaries(apiaryDtos.map((item) => mapApiary(item, allColonies.filter((colony) => colony.apiaryId === item.id).reduce((sum, colony) => sum + colony.count, 0))));
      setColonies(allColonies);
      setSensorReadings(sensorPages.flatMap((page) => page.content));
      setHarvests(harvestDtos.map((item) => mapHarvest(item, apiaryDtos)));
      setExpenses(expenseDtos.map((item) => mapExpense(item, apiaryDtos)));
      setNotifications(notes.content.map(mapNotification));
      setAdminPayments(payments.content);
      setMyPayments([]);
      setAdminInvestments(adminOrders.content.map(mapInvestment));
      setAuditLogs(logs.content);
      setPreferences(prefs);
      setAdminSupportTickets(tickets.content); setSupportTickets([]);
      setInvestments(adminOrders.content.map(mapInvestment)); setInvestorReport(null); setBeekeeperReport(null);
    }
    setLoadError(errors[0] || '');
    setLoading(false);
  };

  useEffect(() => { void refreshData(); }, [role, user.id]);

  useEffect(() => {
    const query = searchQuery.trim();
    if ((!searchOpen && activePage !== 'search') || activePage === 'profile' || !query) {
      setRemoteSearchListings(null);
      setRemoteSearchUsers(null);
      setGlobalSearchLoading(false);
      return;
    }

    let alive = true;
    setRemoteSearchListings(null);
    setRemoteSearchUsers(null);
    setGlobalSearchLoading(true);
    const timer = window.setTimeout(() => {
      const requests: Promise<void>[] = [
        api.listings(query, 0, 100).then((response) => {
          if (alive) setRemoteSearchListings(response.content.map(mapListing));
        }).catch(() => undefined),
      ];
      if (role === 'admin') {
        requests.push(api.adminUsers(query, 0, 100).then((response) => {
          if (alive) setRemoteSearchUsers(response.content);
        }).catch(() => undefined));
      }
      void Promise.all(requests).finally(() => { if (alive) setGlobalSearchLoading(false); });
    }, 180);

    return () => { alive = false; window.clearTimeout(timer); };
  }, [activePage, role, searchOpen, searchQuery]);

  const searchResults = useMemo<SearchResult[]>(() => {
    const query = normalizeSearchText(searchQuery);
    if (!query) return [];
    const results: SearchResult[] = [];
    const add = (item: SearchResult) => {
      if (normalizeSearchText(`${item.category} ${item.title} ${item.subtitle} ${item.searchText}`).includes(query)) results.push(item);
    };
    const listingMap = new Map<string, Listing>();
    [...publicListings, ...listings, ...(remoteSearchListings || [])].forEach((item) => listingMap.set(item.id, item));
    listingMap.forEach((item) => add({
      id: `listing:${item.id}`, category: 'Taklif', title: item.name,
      subtitle: `${item.place} · ${labelStatus(item.status)} · ${formatMoney(item.price)}`,
      searchText: `${item.description} ${item.category} ${item.beekeeper} ${item.id}`,
      page: 'listings', icon: Sprout, listing: item,
    }));
    add({ id: 'profile:me', category: 'Profil', title: user.fullName?.trim() || 'Shaxsiy profil',
      subtitle: `${user.phone}${user.email ? ` · ${user.email}` : ''}`, searchText: `${user.id} ${user.accountStatus}`,
      page: 'profile', icon: UserRound });
    const apiaryNames = new Map(apiaries.map((item) => [item.id, item.name]));
    const colonyCodes = new Map(colonies.map((item) => [item.id, item.code || item.id]));
    apiaries.forEach((item) => add({ id: `apiary:${item.id}`, category: 'Asalarixona', title: item.name,
      subtitle: `${item.place} · ${item.colonies} ta ari oilasi`, searchText: `${item.description || ''} ${item.status} ${item.id}`,
      page: 'apiaries', icon: Building2 }));
    colonies.forEach((item) => add({ id: `colony:${item.id}`, category: 'Ari oilasi', title: item.code || `Ari oilasi · ${item.count} ta`,
      subtitle: `${apiaryNames.get(item.apiaryId) || item.apiaryId} · ${labelStatus(item.status)}`,
      searchText: `${item.description} ${item.strength || ''} ${item.count} ${item.id}`,
      page: 'colonies', icon: Flower2 }));
    investments.forEach((item) => add({ id: `investment:${item.id}`, category: 'Buyurtma', title: item.listingName,
      subtitle: `${item.place} · ${item.status} · ${formatMoney(item.amount)}`,
      searchText: `${item.id} ${item.quantity}`, page: 'investments', icon: BriefcaseBusiness }));
    harvests.forEach((item) => add({ id: `harvest:${item.id}`, category: 'Hosil qaydi', title: `${item.place} · ${item.amount.toLocaleString('uz-UZ')} kg`,
      subtitle: `${item.date} · ${item.status}`, searchText: `${item.id} ${item.note}`,
      page: 'harvests', icon: Droplets }));
    expenses.forEach((item) => add({ id: `expense:${item.id}`, category: 'Xarajat', title: `${item.place} · ${formatMoney(item.amount)}`,
      subtitle: `${item.category} · ${item.date}`, searchText: item.id, page: 'harvests', icon: CreditCard }));
    sensorReadings.forEach((item) => add({ id: `sensor:${item.id}`, category: 'Sensor qaydi', title: `${item.metric === 'TEMPERATURE' ? 'Harorat' : item.metric === 'HUMIDITY' ? 'Namlik' : 'Vazn'} · ${Number(item.value).toLocaleString('uz-UZ')} ${item.unit}`,
      subtitle: `${apiaryNames.get(item.apiaryId) || item.apiaryId} · ${formatDate(item.recordedAt)}`,
      searchText: `${item.id} ${item.source} ${item.colonyId ? colonyCodes.get(item.colonyId) || item.colonyId : ''}`,
      page: 'sensors', icon: Thermometer }));
    notifications.forEach((item) => add({ id: `notification:${item.id}`, category: 'Bildirishnoma', title: item.title,
      subtitle: `${item.time} · ${item.read ? 'O‘qildi' : 'O‘qilmagan'}`, searchText: `${item.body} ${item.kind} ${item.id}`,
      page: 'notifications', icon: Bell }));
    const searchableTickets = role === 'admin' ? adminSupportTickets : supportTickets;
    searchableTickets.forEach((item) => add({ id: `ticket:${item.id}`, category: 'Murojaat', title: item.subject,
      subtitle: `${supportCategory(item.category)} · ${supportStatus(item.status)}`,
      searchText: `${item.message} ${item.adminNote} ${item.id} ${item.userId}`,
      page: 'support', icon: CircleHelp }));
    if (role === 'admin') {
      const searchableUsers = new Map<string, AdminUserDto>();
      [...adminUsers, ...(remoteSearchUsers || [])].forEach((item) => searchableUsers.set(item.id, item));
      searchableUsers.forEach((item) => add({ id: `user:${item.id}`, category: 'Foydalanuvchi',
        title: item.fullName?.trim() || 'Ism kiritilmagan', subtitle: `${item.phone}${item.email ? ` · ${item.email}` : ''}`,
        searchText: `${item.id} ${item.roles.join(' ')} ${item.accountStatus}`,
        page: 'users', icon: Users }));
    }
    const payments = role === 'admin' ? adminPayments : myPayments;
    payments.forEach((item) => add({ id: `payment:${item.id}`, category: 'To‘lov', title: `${formatMoney(Number(item.amount))} · ${labelStatus(item.status)}`,
      subtitle: `${item.provider} · ${formatDate(item.paidAt || item.createdAt)}`,
      searchText: `${item.id} ${item.investmentId} ${item.providerTransactionId || ''} ${item.currency}`,
      page: 'payments', icon: WalletCards }));
    if (role === 'admin') {
      auditLogs.forEach((item) => add({ id: `audit:${item.id}`, category: 'Audit qaydi', title: item.action,
        subtitle: `${item.entityType}${item.entityId ? ` · ${item.entityId}` : ''} · ${item.outcome}`,
        searchText: `${item.actorId || ''} ${JSON.stringify(item.details)} ${item.id}`,
        page: 'audit', icon: ClipboardList }));
    }
    return results.slice(0, 250);
  }, [searchQuery, publicListings, listings, remoteSearchListings, user, apiaries, colonies, investments, harvests, expenses, sensorReadings, notifications, role, adminSupportTickets, supportTickets, remoteSearchUsers, adminUsers, adminPayments, myPayments, auditLogs]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    try {
      if (modal === 'invest' && selectedListing) {
        const quantity = Number(data.get('quantity') || 1);
        await api.createOrder({ listingId: selectedListing.id, unitsQuantity: quantity, idempotencyKey: crypto.randomUUID?.() || `web-${Date.now()}-${Math.random()}` });
        showToast('Buyurtma yaratildi.');
      } else if (modal === 'harvest') {
        await api.createHarvest({ apiaryId: String(data.get('apiaryId') || ''), colonyId: String(data.get('colonyId') || '') || undefined, quantityKg: Number(data.get('amount')), harvestedOn: String(data.get('date')), note: String(data.get('note') || '') });
        showToast('Hosil hisoboti yuborildi.');
      } else if (modal === 'expense') {
        await api.createExpense({ apiaryId: String(data.get('apiaryId') || ''), colonyId: String(data.get('colonyId') || '') || undefined, category: String(data.get('category') || 'Boshqa xarajat'), amount: Number(data.get('amount')), spentOn: String(data.get('date')), note: String(data.get('note') || '') });
        showToast('Xarajat qaydi saqlandi.');
      } else if (modal === 'apiary') {
        const body = { name: String(data.get('name')), location: String(data.get('place')), description: String(data.get('description') || '') };
        if (selectedApiary) { await api.updateApiary(selectedApiary.id, body); showToast('Asalarixona yangilandi.'); }
        else { await api.createApiary(body); showToast('Asalarixona yaratildi.'); }
      } else if (modal === 'colony') {
        const body = { code: String(data.get('code') || '') || undefined, count: Number(data.get('count') || 1), strength: String(data.get('strength') || '') || undefined, description: String(data.get('note') || '') };
        if (selectedColony) { await api.updateColony(selectedColony.id, body); showToast('Ari oilasi yangilandi.'); }
        else { await api.createColony(String(data.get('apiaryId')), body); showToast('Ari oilasi qayd etildi.'); }
      } else if (modal === 'listing') {
        const body = { title: String(data.get('name')), description: String(data.get('description') || ''), location: String(data.get('place')), unitPrice: Number(data.get('price')), unitsTotal: Number(data.get('units')) };
        if (selectedListing) await api.updateListing(selectedListing.id, body);
        else await api.createListing({ apiaryId: String(data.get('apiaryId')), ...body });
        showToast(selectedListing ? 'Taklif yangilandi va moderatsiyaga yuborildi.' : 'Taklif moderatsiyaga yuborildi.');
      } else if (modal === 'sensor') {
        const metric = String(data.get('metric')) as 'TEMPERATURE' | 'HUMIDITY' | 'WEIGHT';
        const unit = metric === 'TEMPERATURE' ? 'C' : metric === 'HUMIDITY' ? '%' : 'kg';
        await api.createSensorReading({ apiaryId: String(data.get('apiaryId')), colonyId: String(data.get('colonyId') || '') || undefined, metric, value: Number(data.get('value')), unit, source: 'MANUAL', recordedAt: new Date().toISOString() });
        showToast('Qo‘lda o‘lchangan sensor qaydi saqlandi.');
      }
      setModal(null);
      await refreshData();
    } catch (issue) { showToast(issue instanceof Error ? issue.message : 'So‘rov bajarilmadi.'); }
  };

  const openListing = (listing: Listing, kind: ModalId = 'details') => { setSelectedListing(listing); setModal(kind); };
  const markAllRead = async () => {
    try { await api.markAllNotificationsRead(); await refreshData(); showToast('Bildirishnomalar o‘qildi deb belgilandi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Amal bajarilmadi.'); }
  };
  const markRead = async (id: string) => {
    try { await api.markNotificationRead(id); await refreshData(); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Amal bajarilmadi.'); }
  };
  const updateListingStatus = async (id: string, status: ListingStatus, note = '') => {
    try { await api.updateListingStatus(id, status === 'Faol' ? 'ACTIVE' : 'DRAFT', note); await refreshData(); showToast(status === 'Faol' ? 'Taklif tasdiqlandi.' : 'Taklif qayta ko‘rib chiqishga qaytarildi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Taklif yangilanmadi.'); }
  };
  const cancelInvestment = async (id: string) => {
    try { await api.cancelOrder(id); await refreshData(); showToast('Buyurtma bekor qilindi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Buyurtmani bekor qilib bo‘lmadi.'); }
  };
  const reviewHarvest = async (id: string, status: 'APPROVED' | 'RETURNED') => {
    try { await api.reviewHarvest(id, status); await refreshData(); showToast(status === 'APPROVED' ? 'Hosil hisoboti tasdiqlandi.' : 'Hisobot tuzatishga qaytarildi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Hosil hisobotini ko‘rib chiqib bo‘lmadi.'); }
  };
  const updateUserStatus = async (id: string, status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED') => {
    try { await api.updateUserStatus(id, status); await refreshData(); showToast('Hisob holati yangilandi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Hisobni yangilab bo‘lmadi.'); }
  };
  const updateUserRoles = async (id: string, roles: string[]) => {
    try { await api.updateUserRoles(id, roles); await refreshData(); showToast('Foydalanuvchi rollari yangilandi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Rollarni yangilab bo‘lmadi.'); }
  };
  const updateProfile = async (fullName: string, email: string, profileImageData: string) => {
    try { const updated = await api.updateProfile(fullName, email, profileImageData); onUserUpdate(updated); showToast('Profil yangilandi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Profilni yangilab bo‘lmadi.'); }
  };
  const deactivateApiary = async (item: Apiary) => {
    if (!window.confirm(`“${item.name}” asalarixonasini faol bo‘lmagan holatga o‘tkazasizmi?`)) return;
    try { await api.deactivateApiary(item.id); await refreshData(); showToast('Asalarixona faol bo‘lmagan holatga o‘tkazildi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Asalarixonani yangilab bo‘lmadi.'); }
  };
  const deactivateColony = async (item: ColonyDto) => {
    if (!window.confirm(`${item.code || 'Ari oilasi'} yozuvini faol bo‘lmagan holatga o‘tkazasizmi?`)) return;
    try { await api.deactivateColony(item.id); await refreshData(); showToast('Ari oilasi faol bo‘lmagan holatga o‘tkazildi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Ari oilasini yangilab bo‘lmadi.'); }
  };
  const createSupportTicket = async (category: SupportTicketDto['category'], subject: string, message: string): Promise<boolean> => {
    try { await api.createSupportTicket({ category, subject, message }); await refreshData(); showToast('Murojaat yuborildi.'); return true; }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Murojaat yuborilmadi.'); return false; }
  };
  const reviewSupportTicket = async (id: string, status: SupportTicketDto['status'], adminNote: string) => {
    try { await api.reviewSupportTicket(id, status, adminNote); await refreshData(); showToast('Murojaat holati yangilandi.'); }
    catch (issue) { showToast(issue instanceof Error ? issue.message : 'Murojaatni yangilab bo‘lmadi.'); }
  };
  const submitGlobalSearch = () => { if (!searchQuery.trim()) { showToast('Qidirish uchun so‘z kiriting.'); return; } setSearchOpen(false); changePage('search'); };
  const selectSearchResult = (result: SearchResult) => { setSearchOpen(false); if (result.listing) { changePage('listings'); openListing(result.listing); } else changePage(result.page); };

  const renderPage = () => {
    if (activePage === 'search') return <SearchResultsPage query={searchQuery} results={searchResults} loading={globalSearchLoading} onSelect={selectSearchResult} />;
    if (activePage === 'public') return <PublicPage listings={publicListings} totalListings={publicListingCount} onBrowse={() => changePage('listings')} onDetail={(item) => openListing(item)} onContact={() => changePage('support')} />;
    if (activePage === 'overview') return <Dashboard role={role} profile={user} investments={role === 'admin' ? adminInvestments : investments} totalInvested={totalInvested} apiaries={apiaries} listings={role === 'beekeeper' ? listings : publicListings} notifications={notifications} investorReport={investorReport} beekeeperReport={beekeeperReport} adminDashboard={adminDashboard} onNavigate={changePage} onOpenModal={setModal} onDetail={(item) => openListing(item)} />;
    if (activePage === 'listings') {
      if (role === 'beekeeper') return <MyListingsPage listings={listings} onCreate={() => { setSelectedListing(null); setModal('listing'); }} onEdit={(item) => openListing(item, 'listing')} />;
      if (role === 'admin') return <ModerationPage listings={listings} onUpdate={updateListingStatus} />;
      return <MarketplacePage listings={listings} query={searchQuery} onQueryChange={setSearchQuery} onDetail={(item) => openListing(item)} />;
    }
    if (activePage === 'investments') return <InvestmentsPage investments={role === 'admin' ? adminInvestments : investments} admin={role === 'admin'} onCancel={cancelInvestment} />;
    if (activePage === 'reports') return <ReportsPage role={role} investments={role === 'admin' ? adminInvestments : investments} harvests={harvests} expenses={expenses} investorReport={investorReport} beekeeperReport={beekeeperReport} adminDashboard={adminDashboard} onExport={() => api.downloadReport(role === 'beekeeper' ? 'beekeeper' : 'investor').catch((issue) => showToast(issue instanceof Error ? issue.message : 'Eksport bajarilmadi.'))} />;
    if (activePage === 'notifications') return <NotificationsPage notifications={notifications} onMarkAllRead={markAllRead} onRead={markRead} />;
    if (activePage === 'support') return role === 'admin' ? <SupportAdminPage tickets={adminSupportTickets} onReview={reviewSupportTicket} /> : <SupportPage tickets={supportTickets} onSubmit={createSupportTicket} />;
    if (activePage === 'apiaries') return <ApiariesPage apiaries={apiaries} onCreate={() => { setSelectedApiary(null); setModal('apiary'); }} onEdit={(item) => { setSelectedApiary(item); setModal('apiary'); }} onDeactivate={(item) => void deactivateApiary(item)} />;
    if (activePage === 'colonies') return <ColoniesPage apiaries={apiaries} colonies={colonies} onCreate={() => { setSelectedColony(null); setModal('colony'); }} onEdit={(item) => { setSelectedColony(item); setModal('colony'); }} onDeactivate={(item) => void deactivateColony(item)} />;
    if (activePage === 'harvests') return role === 'admin' ? <HarvestReviewPage harvests={harvests} onReview={reviewHarvest} /> : <HarvestsPage apiaries={apiaries} harvests={harvests} expenses={expenses} tab={harvestTab} onTabChange={setHarvestTab} onAdd={() => setModal(harvestTab === 'harvest' ? 'harvest' : 'expense')} />;
    if (activePage === 'sensors') return <SensorReadingsPage apiaries={apiaries} colonies={colonies} readings={sensorReadings} onCreate={() => setModal('sensor')} />;
    if (activePage === 'users') return <UsersPage users={adminUsers} currentUserId={user.id} canAssignAdmin={user.roles.includes('SUPER_ADMIN')} onStatus={updateUserStatus} onRoles={updateUserRoles} />;
    if (activePage === 'payments') return role === 'admin' ? <PaymentsPage payments={adminPayments} /> : <InvestorPaymentsPage payments={myPayments} />;
    if (activePage === 'audit') return <AuditPage rows={auditLogs} />;
    if (activePage === 'settings') return <SettingsPage preferences={preferences} onSave={async (value) => { try { await api.updateNotificationPreferences(value); await refreshData(); showToast('Bildirishnoma sozlamalari yangilandi.'); } catch (issue) { showToast(issue instanceof Error ? issue.message : 'Sozlamalar yangilanmadi.'); } }} />;
    return <ProfilePage user={user} role={role} onSave={updateProfile} />;
  };

  const pageTitle = activePage === 'public' ? 'Ochiq sayt' : activePage === 'search' ? 'Qidiruv' : NAV_ITEMS[role].find((item) => item.id === activePage)?.label || (activePage === 'profile' ? 'Profil' : 'Sozlamalar');
  const fullName = accountName;
  const canSeeNav = NAV_ITEMS[role];

  return (
    <div className={`app-shell ${mobileMenuOpen ? 'sidebar-open' : ''}`}>
      <aside className="sidebar">
        <button className="brand" onClick={() => changePage('overview')} aria-label="MELARIUM bosh sahifa">
          <span className="brand-mark"><Hexagon size={31} strokeWidth={1.8} /><span className="brand-mark-dot"><Leaf size={13} fill="currentColor" /></span></span>
          <span className="brand-copy"><strong>MELARIUM</strong><small>ASALARICHILIK EKOTIZIMI</small></span>
        </button>
        <div className="sidebar-label">ISH MAYDONI</div>
        <nav className="side-nav" aria-label="Asosiy navigatsiya">
          {canSeeNav.map((item) => { const Icon = item.icon; return <button key={`${role}-${item.id}`} className={`nav-item ${activePage === item.id ? 'active' : ''}`} onClick={() => changePage(item.id)}><Icon size={18} strokeWidth={1.8} /><span>{item.label}</span>{item.id === 'notifications' && unreadCount > 0 && <span className="nav-count">{unreadCount}</span>}</button>; })}
        </nav>
        <div className="sidebar-label secondary-label">SOZLAMALAR</div>
        <nav className="side-nav">
          <button className={`nav-item ${activePage === 'profile' ? 'active' : ''}`} onClick={() => changePage('profile')}><Settings2 size={18} strokeWidth={1.8} /><span>Profil va sozlamalar</span></button>
          <button className={`nav-item ${activePage === 'settings' ? 'active' : ''}`} onClick={() => changePage('settings')}><Settings2 size={18} strokeWidth={1.8} /><span>Bildirishnoma sozlamalari</span></button>
        </nav>
        <button className={`public-link ${activePage === 'public' ? 'active' : ''}`} onClick={() => changePage('public')}><Globe2 size={17} /><span>Ochiq saytni ko‘rish</span><ArrowUpRight size={15} /></button>
        <div className="sidebar-bottom">
          <div className="help-card"><div className="help-icon"><CircleHelp size={17} /></div><div><strong>Yordam kerakmi?</strong><span>Biz bilan bog‘laning</span></div><ChevronRight size={16} /></div>
          <div className="sidebar-user"><div className="avatar">{user.profileImageData ? <img src={user.profileImageData} alt="" /> : initials(fullName)}</div><div className="sidebar-user-copy"><strong>{fullName}</strong><span>{ROLE_LABELS[role]}</span></div><button className="icon-plain" title="Chiqish" onClick={onLogout}><LogOut size={17} /></button></div>
          <div className="sidebar-footnote">MELARIUM · MVP</div>
        </div>
      </aside>
      {mobileMenuOpen && <button className="mobile-scrim" aria-label="Menyuni yopish" onClick={() => setMobileMenuOpen(false)} />}
      <div className="workspace">
        <header className="topbar">
          <div className="topbar-left"><button className="mobile-menu-button icon-plain" onClick={() => setMobileMenuOpen((open) => !open)} aria-label="Menyuni ochish"><Menu size={21} /></button><div className="breadcrumbs"><span>MELARIUM</span><ChevronRight size={14} /><strong>{pageTitle}</strong></div></div>
          <div className="topbar-actions">
            {activePage !== 'profile' && <form className="global-search" role="search" onSubmit={(event) => { event.preventDefault(); submitGlobalSearch(); }}>
              <button className="global-search-submit" type="submit" aria-label="Butun platformada qidirish"><Search size={17} /></button>
              <input
                type="search"
                role="combobox"
                aria-autocomplete="list"
                aria-expanded={searchOpen && Boolean(searchQuery.trim())}
                aria-controls="global-search-results"
                value={searchQuery}
                onChange={(event) => { const value = event.target.value; setSearchQuery(value); setSearchOpen(Boolean(value.trim())); }}
                onFocus={() => { if (searchQuery.trim()) setSearchOpen(true); }}
                onBlur={() => window.setTimeout(() => setSearchOpen(false), 160)}
                onKeyDown={(event) => { if (event.key === 'Escape') setSearchOpen(false); }}
                placeholder="Butun platformadan qidirish"
                aria-label="Butun platformadan qidirish"
                autoComplete="off"
              />
              {searchQuery && <button className="global-search-clear" type="button" aria-label="Qidiruvni tozalash" onMouseDown={(event) => event.preventDefault()} onClick={() => { setSearchQuery(''); setSearchOpen(false); }}><X size={14} /></button>}
              <kbd aria-hidden="true"><CornerDownLeft size={13} /></kbd>
              {searchOpen && searchQuery.trim() && <div className="global-search-results" id="global-search-results">
                {globalSearchLoading && searchResults.length === 0 ? <div className="global-search-feedback"><LoaderCircle size={14} className="spin" /> Qidirilmoqda…</div> : searchResults.length > 0 ? <>
                  {globalSearchLoading && <div className="global-search-pending"><LoaderCircle size={12} className="spin" /> Natijalar yangilanmoqda</div>}
                  <div className="global-search-options" role="listbox" aria-label="Qidiruv takliflari">
                    {searchResults.slice(0, 7).map((result) => { const Icon = result.icon; return <button type="button" role="option" aria-selected="false" className="global-search-result" key={result.id} onMouseDown={(event) => event.preventDefault()} onClick={() => selectSearchResult(result)}>
                      <span className="global-search-result-icon"><Icon size={15} /></span>
                      <span className="global-search-result-copy"><strong>{result.title}</strong><span>{result.category} · {result.subtitle}</span></span>
                    </button>; })}
                  </div>
                  <button type="button" className="global-search-view-all" onMouseDown={(event) => event.preventDefault()} onClick={submitGlobalSearch}>Barcha natijalarni ko‘rish <ArrowRight size={14} /></button>
                </> : <div className="global-search-feedback">Mos natija topilmadi</div>}
              </div>}
            </form>}
            {availableRoles.length > 1 && <label className="role-control" title="Sizga berilgan rolni tanlang"><span>Rol</span><select value={role} onChange={(event) => handleRoleChange(event.target.value as Role)} aria-label="Mavjud rolni tanlash">{availableRoles.map((item) => <option key={item} value={item}>{ROLE_LABELS[item]}</option>)}</select><ChevronDown size={14} /></label>}
            <button className="topbar-notification" onClick={() => changePage('notifications')} aria-label="Bildirishnomalar"><Bell size={19} />{unreadCount > 0 && <span className="notification-dot">{unreadCount}</span>}</button>
            <div className="topbar-divider" /><button className="top-profile" onClick={() => changePage('profile')}><div className="avatar avatar-small">{user.profileImageData ? <img src={user.profileImageData} alt="" /> : initials(fullName)}</div><div className="top-profile-copy"><strong>{fullName}</strong><span>{ROLE_LABELS[role]}</span></div><ChevronDown size={15} /></button>
          </div>
        </header>
        <main className="main-content">
          {loadError && <div className="load-error-banner" role="status"><CircleHelp size={15} /><span>{loadError}</span><button onClick={() => void refreshData()}>Qayta urinish</button></div>}
          {loading && <div className="inline-loading"><LoaderCircle size={15} className="spin" /> Ma’lumotlar yuklanmoqda…</div>}
          {renderPage()}
        </main>
        <footer className="page-footer"><span>© 2026 MELARIUM</span><span>Raqamli asalarichilik ekotizimi</span><button onClick={() => showToast('Yordam markazi uchun operator bilan bog‘laning.')}><CircleHelp size={14} /> Yordam</button></footer>
      </div>
      <Modal kind={modal} listing={selectedListing} apiary={selectedApiary} colony={selectedColony} apiaries={apiaries} colonies={colonies} canInvest={role === 'investor'} onClose={() => setModal(null)} onSubmit={handleSubmit} onOpenInvest={() => setModal('invest')} />
      {toast && <div className="toast"><span className="toast-check"><Check size={15} /></span>{toast}<button onClick={() => setToast('')} aria-label="Xabarni yopish"><X size={15} /></button></div>}
    </div>
  );
}

function PageHeading({ eyebrow, title, description, action, onAction, actionIcon: ActionIcon = Plus }: { eyebrow?: string; title: string; description?: string; action?: string; onAction?: () => void; actionIcon?: LucideIcon | null }) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {action && <button className="button button-primary" onClick={onAction}>{ActionIcon && <ActionIcon size={17} />}{action}</button>}
    </div>
  );
}

function SearchResultsPage({ query, results, loading, onSelect }: { query: string; results: SearchResult[]; loading: boolean; onSelect: (result: SearchResult) => void }) {
  return (
    <>
      <PageHeading eyebrow="BUTUN PLATFORMADA" title="Qidiruv natijalari" description={query.trim() ? `“${query.trim()}” bo‘yicha ${results.length} ta mos natija.` : 'Qidirish uchun yuqoridagi maydonga so‘z kiriting.'} />
      {!query.trim() ? (
        <div className="empty-state"><Search size={24} /><strong>Qidiruv so‘zini kiriting</strong><span>Takliflar, xo‘jaliklar, buyurtmalar va mavjud yozuvlardan qidirish mumkin.</span></div>
      ) : results.length === 0 && loading ? (
        <div className="search-loading"><LoaderCircle size={19} className="spin" /> Ma’lumotlar qidirilmoqda…</div>
      ) : results.length === 0 ? (
        <div className="empty-state"><Search size={24} /><strong>Natija topilmadi</strong><span>Boshqa so‘z yoki telefon raqamini sinab ko‘ring.</span></div>
      ) : (
        <section className="search-results-list" aria-label="Qidiruv natijalari">
          {results.map((result) => {
            const Icon = result.icon;
            return <button className="search-result-item" key={result.id} onClick={() => onSelect(result)}>
              <span className="search-result-icon"><Icon size={18} strokeWidth={1.9} /></span>
              <span className="search-result-copy"><span className="search-result-category">{result.category}</span><strong>{result.title}</strong><span>{result.subtitle}</span></span>
              <ArrowRight className="search-result-arrow" size={16} />
            </button>;
          })}
        </section>
      )}
    </>
  );
}

function StatCard({ title, value, helper, icon: Icon, tone = 'green', trend }: { title: string; value: string; helper: string; icon: LucideIcon; tone?: string; trend?: string }) {
  return (
    <div className="stat-card">
      <div className={`stat-icon tone-${tone}`}><Icon size={19} strokeWidth={1.9} /></div>
      {trend && <span className="stat-trend"><ArrowUpRight size={13} />{trend}</span>}
      <div className="stat-title">{title}</div>
      <div className="stat-value">{value}</div>
      <div className="stat-helper">{helper}</div>
    </div>
  );
}

function Dashboard({ role, profile, investments, totalInvested, apiaries, listings, notifications, investorReport, beekeeperReport, adminDashboard, onNavigate, onOpenModal, onDetail }: {
  role: Role;
  profile: UserProfile;
  investments: Investment[];
  totalInvested: number;
  apiaries: Apiary[];
  listings: Listing[];
  notifications: NotificationItem[];
  investorReport: InvestorReportDto | null;
  beekeeperReport: BeekeeperReportDto | null;
  adminDashboard: AdminDashboardDto | null;
  onNavigate: (page: PageId) => void;
  onOpenModal: (modal: ModalId) => void;
  onDetail: (listing: Listing) => void;
}) {
  if (role === 'user') return <BasicUserOverview profile={profile} listings={listings} notifications={notifications} onNavigate={onNavigate} onDetail={onDetail} />;
  const activeListings = listings.filter((item) => item.status === 'Faol');
  const today = formatToday();
  const activeInvestmentCount = investorReport?.activeInvestments ?? investments.filter((item) => item.status === 'Faol').length;
  const orderCount = investments.length;
  const configs = role === 'investor'
    ? [
        { title: 'Faol buyurtmalar qiymati', value: investorReport ? formatShortMoney(Number(investorReport.activePrincipalAmount)) : formatShortMoney(totalInvested), helper: 'Faol investitsiya yozuvlari', icon: WalletCards, tone: 'green' },
        { title: 'Faol investitsiyalar', value: `${activeInvestmentCount} ta`, helper: `${orderCount} ta buyurtma yozuvi`, icon: Sprout, tone: 'honey' },
        { title: 'Kutilayotgan buyurtma', value: `${investorReport?.pendingOrders ?? 0} ta`, helper: 'To‘lov hali tasdiqlanmagan', icon: Clock3, tone: 'blue' },
        { title: 'Xo‘jalik hisobotlari', value: `${investorReport?.apiaryReports.length ?? 0} ta`, helper: 'Holatini hisobotdan tekshiring', icon: FileText, tone: 'lavender' },
      ]
    : role === 'beekeeper'
      ? [
          { title: 'Ari oilalari', value: `${beekeeperReport?.colonyCount ?? apiaries.reduce((sum, item) => sum + item.colonies, 0)} ta`, helper: `${beekeeperReport?.apiaryCount ?? apiaries.length} ta asalarixonada`, icon: Flower2, tone: 'green' },
          { title: 'Faol takliflar', value: `${beekeeperReport?.activeListings ?? activeListings.length} ta`, helper: 'Investorlar uchun ochiq', icon: Sprout, tone: 'honey' },
          { title: 'Qayd etilgan hosil', value: beekeeperReport ? `${Number(beekeeperReport.harvestKg).toLocaleString('uz-UZ')} kg` : '—', helper: 'Hisobotdagi jami miqdor', icon: Droplets, tone: 'blue' },
          { title: 'Xarajatlar', value: beekeeperReport ? formatShortMoney(Number(beekeeperReport.expensesUzs)) : '—', helper: 'Qayd etilgan xo‘jalik xarajatlari', icon: CreditCard, tone: 'lavender' },
        ]
      : [
          { title: 'Foydalanuvchilar', value: `${adminDashboard?.totalUsers ?? 0}`, helper: `${adminDashboard?.activeUsers ?? 0} faol hisob`, icon: Users, tone: 'green' },
          { title: 'Moderatsiya navbati', value: `${adminDashboard?.pendingListings ?? 0} ta`, helper: 'Takliflar qaror kutmoqda', icon: ShieldCheck, tone: 'honey' },
          { title: 'Faol takliflar', value: `${adminDashboard?.activeListings ?? activeListings.length} ta`, helper: 'Bozorda ko‘rinayotgan takliflar', icon: Sprout, tone: 'blue' },
          { title: 'To‘lov ko‘rib chiqishda', value: `${adminDashboard?.paymentReviewOrders ?? adminDashboard?.pendingPayments ?? 0} ta`, helper: 'To‘lov holati', icon: CreditCard, tone: 'lavender' },
        ];
  const recentNotifications = notifications.slice(0, 3);
  const portfolio = role === 'investor'
    ? [['Faol investitsiyalar', `${activeInvestmentCount} ta`, 'legend-green'], ['Kutilayotgan buyurtmalar', `${investorReport?.pendingOrders ?? 0} ta`, 'legend-honey'], ['Bekor qilingan', `${investorReport?.cancelledOrders ?? 0} ta`, 'legend-grey']]
    : role === 'beekeeper'
      ? [['Faol takliflar', `${beekeeperReport?.activeListings ?? activeListings.length} ta`, 'legend-green'], ['Moderatsiyada', `${beekeeperReport?.pendingListings ?? listings.filter((item) => item.status === 'Moderatsiyada').length} ta`, 'legend-honey'], ['Asalarixonalar', `${beekeeperReport?.apiaryCount ?? apiaries.length} ta`, 'legend-grey']]
      : [['Faol takliflar', `${adminDashboard?.activeListings ?? 0} ta`, 'legend-green'], ['Moderatsiya', `${adminDashboard?.pendingListings ?? 0} ta`, 'legend-honey'], ['To‘lov tekshiruvi', `${adminDashboard?.paymentReviewOrders ?? 0} ta`, 'legend-grey']];

  return (
    <>
      <PageHeading eyebrow={`${ROLE_LABELS[role].toUpperCase()} KABINETI  ·  ${today.toUpperCase()}`} title={role === 'admin' ? 'Platforma holati' : `Assalomu alaykum, ${profile.fullName?.trim() || profile.phone}`} description={role === 'investor' ? 'Buyurtmalar va xo‘jalik hisobotlari.' : role === 'beekeeper' ? 'Asalarixonalar, ari oilalari, hosil va xarajatlaringiz.' : 'Platforma ko‘rsatkichlari va boshqaruv navbati.'} action={role === 'beekeeper' ? 'Hisobot qo‘shish' : role === 'admin' ? 'Takliflarni ko‘rish' : 'Takliflarni ko‘rish'} onAction={() => role === 'beekeeper' ? (apiaries.length ? onOpenModal('harvest') : onNavigate('apiaries')) : onNavigate('listings')} />

      {role !== 'admin' && <HeroBanner role={role} onClick={() => onNavigate(role === 'investor' ? 'listings' : 'apiaries')} />}

      <div className="stats-grid">{configs.map((card) => <StatCard key={card.title} {...card} />)}</div>

      <div className="dashboard-main-grid">
        <section className="panel performance-panel">
          <div className="panel-heading"><div><div className="panel-kicker">FAOLIYAT</div><h2>{role === 'investor' ? 'Portfel va buyurtmalar' : role === 'beekeeper' ? 'Xo‘jalik hisobotlari' : 'Platforma holati'}</h2><p>Joriy faoliyat ko‘rsatkichlari.</p></div></div>
          <div className="summary-strip">{role === 'investor' ? <><div><span>Faol buyurtmalar qiymati</span><strong>{formatMoney(Number(investorReport?.activePrincipalAmount ?? totalInvested))}</strong></div><div><span>Kutilayotgan buyurtma summasi</span><strong>{formatMoney(Number(investorReport?.pendingOrderAmount ?? 0))}</strong></div></> : role === 'beekeeper' ? <><div><span>Hosil (hisobot)</span><strong>{beekeeperReport ? `${Number(beekeeperReport.harvestKg).toLocaleString('uz-UZ')} kg` : '—'}</strong></div><div><span>Xarajatlar (hisobot)</span><strong>{beekeeperReport ? formatMoney(Number(beekeeperReport.expensesUzs)) : '—'}</strong></div></> : <><div><span>Foydalanuvchilar</span><strong>{adminDashboard?.totalUsers ?? '—'}</strong></div><div><span>To‘lovlar ko‘rib chiqishda</span><strong>{adminDashboard?.paymentReviewOrders ?? '—'}</strong></div></>}</div>
        </section>
        <section className="panel insight-panel">
          <div className="panel-heading compact-heading"><div><div className="panel-kicker">QISQA TAHLIL</div><h2>{role === 'investor' ? 'Buyurtmalar holati' : role === 'beekeeper' ? 'Xo‘jalik holati' : 'Boshqaruv navbati'}</h2></div></div>
          <div className="portfolio-ring-wrap"><div className="portfolio-ring"><div className="portfolio-ring-center"><strong>{role === 'investor' ? activeInvestmentCount : role === 'beekeeper' ? (beekeeperReport?.colonyCount ?? 0) : (adminDashboard?.pendingListings ?? 0)}</strong><span>{role === 'investor' ? 'faol' : role === 'beekeeper' ? 'oila' : 'navbatda'}</span></div></div><div className="ring-caption"><strong>{role === 'investor' ? 'Buyurtmalar' : role === 'beekeeper' ? 'Xo‘jaliklar' : 'Admin nazorati'}</strong><span>{role === 'investor' ? 'Hisobotdagi holatlar' : 'Joriy holat'}</span></div></div>
          <div className="legend-list">{portfolio.map(([label, value, dot]) => <div className="legend-row" key={label}><span className={`legend-dot ${dot}`} /><span>{label}</span><strong>{value}</strong></div>)}</div>
          <button className="text-link" onClick={() => onNavigate(role === 'investor' ? 'investments' : role === 'beekeeper' ? 'colonies' : 'listings')}>Batafsil ko‘rish <ArrowRight size={15} /></button>
        </section>
      </div>

      <div className="dashboard-lower-grid">
        <section className="panel table-panel">
          <div className="panel-heading"><div><div className="panel-kicker">RO‘YXAT</div><h2>{role === 'admin' ? 'Investitsiya buyurtmalari' : role === 'beekeeper' ? 'Asalarixonalarim' : 'Investitsiyalarim'}</h2><p>{role === 'investor' ? 'Buyurtma va holatlar ro‘yxati' : role === 'beekeeper' ? 'Xo‘jaliklaringiz bo‘yicha ma’lumot' : 'Administrator ko‘rayotgan yozuvlar'}</p></div><button className="text-link" onClick={() => onNavigate(role === 'beekeeper' ? 'apiaries' : 'investments')}>Barchasi <ArrowRight size={15} /></button></div>
          {role === 'beekeeper' ? <ApiaryMiniTable apiaries={apiaries.slice(0, 3)} /> : <InvestmentMiniTable investments={investments.slice(0, 3)} admin={role === 'admin'} />}
          {role !== 'beekeeper' && investments.length === 0 && <div className="empty-inline">Hozircha buyurtma yo‘q.</div>}
        </section>
        <section className="panel activity-panel">
          <div className="panel-heading"><div><div className="panel-kicker">BILDIRISHNOMALAR</div><h2>So‘nggi faollik</h2><p>Buyurtma va xo‘jalik yangilanishlari</p></div></div>
          <div className="activity-list">{recentNotifications.length ? recentNotifications.map((item) => { const Icon = item.kind === 'payment' ? CircleDollarSign : item.kind === 'report' ? FileText : item.kind === 'listing' ? Sprout : Bell; const color = item.kind === 'payment' ? 'honey' : item.kind === 'report' ? 'blue' : 'green'; return <ActivityItem key={item.id} icon={Icon} color={color} title={item.title} subtitle={item.body} time={item.time} />; }) : <div className="empty-inline">Hozircha bildirishnoma yo‘q.</div>}</div>
          <button className="text-link activity-link" onClick={() => onNavigate('notifications')}>Barcha bildirishnomalar <ArrowRight size={15} /></button>
        </section>
      </div>

      {role === 'investor' && <section className="panel recommendation-panel"><div className="panel-heading"><div><div className="panel-kicker">FAOL TAKLIFLAR</div><h2>Asalarichilik takliflari</h2><p>Bozordagi faol takliflar</p></div><button className="text-link" onClick={() => onNavigate('listings')}>Bozorga o‘tish <ArrowRight size={15} /></button></div>{activeListings.length ? <div className="recommendation-row">{activeListings.slice(0, 3).map((item) => <MiniListingCard key={item.id} item={item} onClick={() => onDetail(item)} />)}</div> : <div className="empty-inline">Hozircha faol taklif yo‘q.</div>}</section>}
    </>
  );
}

function BasicUserOverview({ profile, listings, notifications, onNavigate, onDetail }: { profile: UserProfile; listings: Listing[]; notifications: NotificationItem[]; onNavigate: (page: PageId) => void; onDetail: (listing: Listing) => void }) {
  const active = listings.filter((item) => item.status === 'Faol');
  return <><PageHeading eyebrow="SHAXSIY KABINET" title={`Xush kelibsiz, ${profile.fullName?.trim() || profile.phone}`} description="Sizda hozircha oddiy foydalanuvchi roli mavjud. Investor yoki asalarichi xizmatlari uchun administrator sizga tegishli rolni biriktiradi." /><div className="notice-card"><CircleHelp size={19} /><div><strong>Rol tayinlanishini kuting</strong><span>Bu hisob bilan faol takliflarni ko‘rish, bildirishnomalarni boshqarish va yordam/shikoyat yuborish mumkin. Xarid yoki xo‘jalik boshqaruvi uchun vakolatli admin rol beradi.</span></div><button className="button button-outline" onClick={() => onNavigate('support')}>Murojaat yuborish</button></div><div className="stats-grid compact-stats"><StatCard title="Faol takliflar" value={`${active.length} ta`} helper="Takliflar soni" icon={Sprout} tone="green" /><StatCard title="Bildirishnomalar" value={`${notifications.length} ta`} helper="Hisobingizga bog‘langan" icon={Bell} tone="blue" /><StatCard title="Hisob holati" value={labelStatus(profile.accountStatus)} helper="Hisob holati" icon={ShieldCheck} tone="honey" /></div><section className="panel recommendation-panel"><div className="panel-heading"><div><div className="panel-kicker">PUBLIC MARKETPLACE</div><h2>Faol takliflar</h2><p>Takliflarni ko‘rish va tafsilotlarini tekshirish mumkin.</p></div><button className="text-link" onClick={() => onNavigate('listings')}>Barcha takliflar <ArrowRight size={15} /></button></div>{active.length ? <div className="recommendation-row">{active.slice(0, 3).map((item) => <MiniListingCard key={item.id} item={item} onClick={() => onDetail(item)} />)}</div> : <div className="empty-inline">Hozircha faol taklif yo‘q.</div>}</section></>;
}

function HeroBanner({ role, onClick }: { role: Role; onClick: () => void }) {
  const investor = role === 'investor';
  return (
    <section className="hero-banner">
      <div className="hero-photo" />
      <div className="hero-shade" />
      <div className="hero-content">
        <span className="hero-badge"><Leaf size={13} /> {investor ? 'TABIIY HAMKORLIK' : 'MAVSUMIY BOSHQARUV'}</span>
        <h2>{investor ? <>Asalarichilik kelajagiga<br />ongli sarmoya kiriting.</> : <>Har bir ari oilasini<br />ishonch bilan boshqaring.</>}</h2>
        <p>{investor ? 'Shaffof hisobotlar orqali xo‘jaliklar rivojini kuzatib boring.' : 'Xo‘jalik faoliyati, hosil va xarajatlarni bir joyda qayd eting.'}</p>
        <button onClick={onClick}>{investor ? 'Imkoniyatlarni ko‘rish' : 'Xo‘jaliklarni ko‘rish'} <ArrowRight size={16} /></button>
      </div>
      <div className="hero-index"><span>01</span><i /><span>03</span></div>
    </section>
  );
}

function InvestmentMiniTable({ investments, admin = false, onCancel }: { investments: Investment[]; admin?: boolean; onCancel?: (id: string) => void }) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead><tr><th>{admin ? 'Investor / loyiha' : 'Loyiha'}</th><th>Sana</th><th>Miqdor</th><th>Holat</th><th>Amal</th></tr></thead>
        <tbody>{investments.map((item) => <tr key={item.id}>
          <td><div className="table-primary">{item.listingName}</div><div className="table-secondary">{item.place}</div></td>
          <td className="muted-cell">{item.date}</td>
          <td className="strong-cell">{formatMoney(item.amount)}</td>
          <td><span className={`status-chip ${item.status === 'Faol' ? 'status-green' : 'status-honey'}`}><span />{item.status}</span></td>
          <td>{onCancel && item.status === 'To‘lov kutilmoqda' ? <button className="button button-outline table-cancel" onClick={() => window.confirm('Vaqtinchalik buyurtmani bekor qilasizmi? To‘lov qilinmagan.') && onCancel(item.id)}>Bekor qilish</button> : <span className="muted-cell">—</span>}</td>
        </tr>)}</tbody>
      </table>
    </div>
  );
}

function ApiaryMiniTable({ apiaries }: { apiaries: Apiary[] }) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead><tr><th>Asalarixona</th><th>Joylashuv</th><th>Oilalar</th><th>Holat</th></tr></thead>
        <tbody>{apiaries.map((item) => <tr key={item.id}><td><div className="table-primary">{item.name}</div><div className="table-secondary">ID: {item.id}</div></td><td className="muted-cell">{item.place}</td><td className="strong-cell">{item.colonies} ta</td><td><span className="status-chip status-green"><span />{item.status}</span></td></tr>)}</tbody>
      </table>
    </div>
  );
}

function ActivityItem({ icon: Icon, color, title, subtitle, time }: { icon: LucideIcon; color: string; title: string; subtitle: string; time: string }) {
  return <div className="activity-item"><span className={`activity-icon activity-${color}`}><Icon size={16} /></span><div className="activity-copy"><strong>{title}</strong><span>{subtitle}</span></div><time>{time}</time></div>;
}

function MiniListingCard({ item, onClick }: { item: Listing; onClick: () => void }) {
  return (
    <button className="mini-listing" onClick={onClick}>
      <div className="mini-listing-image"><img src="/apiary.jpg" alt="Asalarichilik xo‘jaligi" /><span><Sprout size={12} /> Faol taklif</span></div>
      <div className="mini-listing-content"><div className="mini-listing-place"><MapPin size={12} />{item.place.split(',')[0]}</div><strong>{item.name}</strong><div className="mini-listing-bottom"><span>{formatMoney(item.price)} <small>/ oila</small></span><span className="mini-arrow"><ArrowRight size={15} /></span></div></div>
    </button>
  );
}

function MarketplacePage({ listings, query, onQueryChange, onDetail }: { listings: Listing[]; query: string; onQueryChange: (query: string) => void; onDetail: (item: Listing) => void }) {
  const [category, setCategory] = useState('Barchasi');
  const [serverMatches, setServerMatches] = useState<Listing[] | null>(null);
  const [searchLoading, setSearchLoading] = useState(false);

  useEffect(() => {
    const term = query.trim();
    if (!term) {
      setServerMatches(null);
      setSearchLoading(false);
      return;
    }
    let alive = true;
    setServerMatches(null);
    setSearchLoading(true);
    const timer = window.setTimeout(() => {
      api.listings(term, 0, 100).then((response) => {
        if (alive) setServerMatches(response.content.map(mapListing));
      }).catch(() => {
        if (alive) setServerMatches(null);
      }).finally(() => {
        if (alive) setSearchLoading(false);
      });
    }, 250);
    return () => { alive = false; window.clearTimeout(timer); };
  }, [query]);

  const searchPool = useMemo(() => {
    const unique = new Map<string, Listing>();
    [...listings, ...(serverMatches || [])].forEach((item) => unique.set(item.id, item));
    return [...unique.values()];
  }, [listings, serverMatches]);
  const categories = useMemo(() => ['Barchasi', ...new Set(searchPool.filter((item) => item.status === 'Faol').map((item) => item.category))], [searchPool]);
  const filtered = useMemo(() => {
    const normalizedQuery = normalizeSearchText(query);
    return searchPool.filter((item) => {
      const searchable = normalizeSearchText(`${item.name} ${item.place} ${item.beekeeper} ${item.description} ${item.category} ${item.id}`);
      const matchesQuery = !normalizedQuery || searchable.includes(normalizedQuery);
      const matchesCategory = category === 'Barchasi' || item.category === category;
      return item.status === 'Faol' && matchesQuery && matchesCategory;
    });
  }, [searchPool, query, category]);

  return (
    <>
      <PageHeading eyebrow="INVESTOR UCHUN" title="Asalarichilik imkoniyatlari" description="Asalarichilik xo‘jaliklarining faol takliflari va tafsilotlarini ko‘ring." />
      <section className="marketplace-banner">
        <div className="marketplace-banner-content"><span className="marketplace-pill"><Sprout size={13} /> Faol takliflar</span><h2>Mahalliy asalarichilik<br />takliflarini o‘rganing.</h2><p>Taklifning narxi, joylashuvi va mavjudligini mavjud ma’lumotlar asosida ko‘ring.</p><div className="marketplace-banner-points"><span><CheckCircle2 size={14} /> Ochiq taklif tafsilotlari</span><span><CheckCircle2 size={14} /> Buyurtma holati</span></div></div>
        <div className="marketplace-banner-photo" />
      </section>
      <div className="market-toolbar">
        <div><div className="eyebrow">BOZOR</div><h2>Mavjud takliflar <span className="count-badge">{filtered.length}</span></h2></div>
        <div className="market-controls">
          <label className="market-search"><Search size={16} /><input type="search" value={query} onChange={(event) => onQueryChange(event.target.value)} placeholder="Xo‘jalik yoki joylashuv" aria-label="Takliflar va joylashuvni qidirish" /></label>
        </div>
      </div>
      <div className="filter-tabs" aria-label="Taklif turi bo‘yicha filtr">{categories.map((item) => <button type="button" key={item} className={category === item ? 'selected' : ''} aria-pressed={category === item} onClick={() => setCategory(item)}>{item}</button>)}</div>
      {filtered.length === 0 && searchLoading ? <div className="search-loading"><LoaderCircle size={18} className="spin" /> Takliflar qidirilmoqda…</div> : filtered.length === 0 ? <div className="empty-state"><Search size={25} /><strong>Taklif topilmadi</strong><span>Qidiruv so‘zini yoki filtrni o‘zgartirib ko‘ring.</span></div> : <div className="listing-grid">{filtered.map((item, index) => <ListingCard key={item.id} item={item} index={index} onClick={() => onDetail(item)} />)}</div>}
    </>
  );
}

function ListingCard({ item, index, onClick }: { item: Listing; index: number; onClick: () => void }) {
  const imagePosition = index % 2 === 0 ? 'center' : 'right';
  return (
    <article className="listing-card">
      <button className="listing-image" onClick={onClick} aria-label={`${item.name} tafsiloti`}><img src="/apiary.jpg" alt="Asalarixona va ari uyalari" style={{ objectPosition: imagePosition }} /><span className="verified-tag"><Sprout size={13} /> Faol taklif</span><span className="image-units">{item.units} oila mavjud</span></button>
      <div className="listing-body"><div className="listing-location"><MapPin size={13} />{item.place}</div><button className="listing-title-button" onClick={onClick}><h3>{item.name}</h3><ArrowUpRight size={16} /></button><div className="listing-beekeeper"><div className="avatar avatar-tiny">{item.beekeeper.split(' ').map((part) => part[0]).join('').slice(0, 2)}</div><span>Asalarichi</span><strong>{item.beekeeper}</strong></div><div className="listing-divider" /><div className="listing-stats"><div><span>Narxi / oila</span><strong>{formatMoney(item.price)}</strong></div><div><span>Taklif turi</span><strong>{item.category}</strong></div></div><button className="button button-light listing-cta" onClick={onClick}>Tafsilotlarni ko‘rish <ArrowRight size={15} /></button></div>
    </article>
  );
}

function MyListingsPage({ listings, onCreate, onEdit }: { listings: Listing[]; onCreate: () => void; onEdit: (item: Listing) => void }) {
  const ownListings = listings;
  return <>
    <PageHeading eyebrow="ASALARICHI KABINETI" title="Mening takliflarim" description="Takliflarni boshqaring. Tahrirlangan taklif yana moderatsiyaga yuboriladi." action="Yangi taklif" onAction={onCreate} />
    <div className="notice-card"><ShieldCheck size={19} /><div><strong>Moderatsiya jarayoni</strong><span>Yangi yoki tahrirlangan taklif admin tekshiruvidan so‘ng investorlar bozorida ko‘rinadi.</span></div><span className="notice-tag">{ownListings.filter((item) => item.status === 'Moderatsiyada').length} ta navbatda</span></div>
    {ownListings.length === 0 ? <div className="empty-state"><Sprout size={25} /><strong>Hozircha taklif yo‘q</strong><span>Yangi taklif yaratib, moderatsiyaga yuboring.</span><button className="button button-primary" onClick={onCreate}><Plus size={16} /> Taklif yaratish</button></div> : <div className="table-panel panel"><div className="table-scroll"><table className="data-table"><thead><tr><th>Taklif</th><th>Narx / birlik</th><th>Mavjudligi</th><th>Holati</th><th>Yaratilgan</th><th>Amal</th></tr></thead><tbody>{ownListings.map((item) => <tr key={item.id}><td><div className="table-primary">{item.name}</div><div className="table-secondary">{item.place}</div></td><td className="strong-cell">{formatMoney(item.price)}</td><td>{item.units} ta</td><td><span className={`status-chip ${item.status === 'Faol' ? 'status-green' : 'status-honey'}`}><span />{item.status}</span></td><td className="muted-cell">{item.createdAt ? formatDate(item.createdAt) : '—'}</td><td>{item.status === 'Band' || item.status === 'Sotilgan' ? <span className="muted-cell">Tahrirlab bo‘lmaydi</span> : <button className="button button-outline table-cancel" onClick={() => onEdit(item)}>Tahrirlash</button>}</td></tr>)}</tbody></table></div></div>}
    <div className="stats-grid compact-stats"><StatCard title="Jami takliflar" value={`${ownListings.length} ta`} helper="Qayd etilgan" icon={Sprout} tone="green" /><StatCard title="Moderatsiyada" value={`${ownListings.filter((item) => item.status === 'Moderatsiyada').length} ta`} helper="Admin ko‘rib chiqadi" icon={Clock3} tone="honey" /><StatCard title="Faol" value={`${ownListings.filter((item) => item.status === 'Faol').length} ta`} helper="Bozorda ko‘rinadi" icon={Users} tone="blue" /></div>
  </>;
}

function ModerationPage({ listings, onUpdate }: { listings: Listing[]; onUpdate: (id: string, status: ListingStatus, note?: string) => void }) {
  const [notes, setNotes] = useState<Record<string, string>>({});
  const pending = listings.filter((item) => item.status === 'Moderatsiyada' || item.status === 'Qoralama');
  return <>
    <PageHeading eyebrow="ADMIN PANEL" title="Takliflar nazorati" description="Taklifni tekshirib, qaror va zarur bo‘lsa tuzatish izohini qayd eting." />
    <div className="admin-summary"><div><span className="admin-summary-icon honey"><Clock3 size={18} /></span><div><small>Ko‘rib chiqish kutilmoqda</small><strong>{pending.length} ta taklif</strong></div></div></div>
    <div className="moderation-list">{pending.length === 0 ? <div className="empty-state"><CheckCircle2 size={24} /><strong>Navbat bo‘sh</strong><span>Yangi taklif kelganda bu yerda ko‘rsatiladi.</span></div> : pending.map((item) => <article className="moderation-card" key={item.id}><img src="/apiary.jpg" alt="Asalarichilik xo‘jaligi" /><div className="moderation-main"><span className="status-chip status-honey"><span />{item.status}</span><h3>{item.name}</h3><p><MapPin size={13} /> {item.place}</p><div className="moderation-meta"><span>Taklif egasi ID: <strong>{item.beekeeper}</strong></span><span>Narxi: <strong>{formatMoney(item.price)}</strong></span><span>Mavjud: <strong>{item.units} birlik</strong></span></div><label className="moderation-note-label">Qaror izohi (ixtiyoriy)<textarea value={notes[item.id] || ''} maxLength={1000} rows={2} onChange={(event) => setNotes((current) => ({ ...current, [item.id]: event.target.value }))} placeholder="Qayta ko‘rib chiqishda talab qilinadigan tuzatish" /></label></div><div className="moderation-actions"><button className="button button-primary" onClick={() => onUpdate(item.id, 'Faol', notes[item.id] || '')}><Check size={15} /> Tasdiqlash</button><button className="button button-outline" onClick={() => onUpdate(item.id, 'Qoralama', notes[item.id] || '')}>Tuzatishga qaytarish</button></div></article>)}</div>
  </>;
}

function InvestmentsPage({ investments, admin = false, onCancel }: { investments: Investment[]; admin?: boolean; onCancel: (id: string) => void }) {
  const active = investments.filter((item) => item.status === 'Faol');
  const pending = investments.filter((item) => item.status === 'To‘lov kutilmoqda');
  const totalPrincipal = active.reduce((sum, item) => sum + item.amount, 0);
  return <>
    <PageHeading eyebrow={admin ? 'ADMIN PANEL' : 'INVESTOR KABINETI'} title={admin ? 'Investitsiya buyurtmalari' : 'Buyurtmalarim'} description={admin ? 'Buyurtmalar va faol holatlarni ko‘ring.' : 'Buyurtmalar va ularning holatini ko‘ring.'} />
    <div className="stats-grid compact-stats"><StatCard title="Faol buyurtmalar qiymati" value={formatShortMoney(totalPrincipal)} helper="Faol yozuvlar" icon={WalletCards} tone="green" /><StatCard title="Faol investitsiyalar" value={`${active.length} ta`} helper="Faol buyurtmalar" icon={Sprout} tone="honey" /><StatCard title="To‘lov kutilmoqda" value={`${pending.length} ta`} helper="Kutilayotgan buyurtmalar" icon={Clock3} tone="blue" /></div>
    <section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">BUYURTMALAR</div><h2>Barcha yozuvlar</h2></div>{!admin && <button className="button button-outline" onClick={() => api.downloadReport('investor').catch(() => undefined)}><Download size={15} /> CSV eksport</button>}</div>{investments.length ? <InvestmentMiniTable investments={investments} admin={admin} onCancel={admin ? undefined : onCancel} /> : <div className="empty-state"><BriefcaseBusiness size={24} /><strong>Buyurtma yoki investitsiya yo‘q</strong><span>Faol taklifni tanlab, buyurtma yarating.</span></div>}</section>
  </>;
}

function ReportsPage({ role, investments, harvests, expenses, investorReport, beekeeperReport, adminDashboard, onExport }: {
  role: Role;
  investments: Investment[];
  harvests: HarvestRecord[];
  expenses: ExpenseRecord[];
  investorReport: InvestorReportDto | null;
  beekeeperReport: BeekeeperReportDto | null;
  adminDashboard: AdminDashboardDto | null;
  onExport: () => void | Promise<void>;
}) {
  const cards = role === 'investor'
    ? [
        { title: 'Faol buyurtmalar qiymati', value: investorReport ? formatMoney(Number(investorReport.activePrincipalAmount)) : '—', helper: 'Faol investitsiya yozuvlari', icon: WalletCards, tone: 'green' },
        { title: 'Kutilayotgan buyurtma', value: investorReport ? formatMoney(Number(investorReport.pendingOrderAmount)) : '—', helper: `${investorReport?.pendingOrders ?? 0} ta buyurtma`, icon: Clock3, tone: 'honey' },
        { title: 'Xo‘jalik hosil qaydlari', value: `${investorReport?.apiaryReports.length ?? 0} ta`, helper: 'Investor hisobotiga kiritilgan', icon: Droplets, tone: 'blue' },
      ]
    : role === 'beekeeper'
      ? [
          { title: 'Jami hosil', value: beekeeperReport ? `${Number(beekeeperReport.harvestKg).toLocaleString('uz-UZ')} kg` : `${harvests.reduce((sum, item) => sum + item.amount, 0)} kg`, helper: 'Hisobotdagi jami ko‘rsatkich', icon: Droplets, tone: 'green' },
          { title: 'Jami xarajat', value: beekeeperReport ? formatMoney(Number(beekeeperReport.expensesUzs)) : formatMoney(expenses.reduce((sum, item) => sum + item.amount, 0)), helper: 'Qayd etilgan so‘m xarajatlari', icon: CreditCard, tone: 'honey' },
          { title: 'Faol takliflar', value: beekeeperReport ? `${beekeeperReport.activeListings} ta` : '—', helper: `${beekeeperReport?.pendingListings ?? 0} ta moderatsiyada`, icon: Sprout, tone: 'blue' },
        ]
      : [
          { title: 'Foydalanuvchilar', value: `${adminDashboard?.totalUsers ?? '—'}`, helper: `${adminDashboard?.activeUsers ?? 0} faol hisob`, icon: Users, tone: 'green' },
          { title: 'Faol takliflar', value: `${adminDashboard?.activeListings ?? '—'}`, helper: `${adminDashboard?.pendingListings ?? 0} ta moderatsiyada`, icon: Sprout, tone: 'honey' },
          { title: 'Faol investitsiyalar', value: `${adminDashboard?.activeInvestments ?? investments.filter((item) => item.status === 'Faol').length}`, helper: 'Faol holatlar', icon: TrendingUp, tone: 'blue' },
        ];
  return <>
    <PageHeading eyebrow={role === 'admin' ? 'PLATFORMA' : 'TAHLIL VA HISOBOT'} title={role === 'beekeeper' ? 'Hosil va xo‘jalik tahlili' : role === 'admin' ? 'Platforma analitikasi' : 'Investitsiya hisobotlari'} description="Hisobotlar va ularga bog‘liq yozuvlar." />
    <div className="report-toolbar"><div className="report-toolbar-note"><CalendarDays size={16} /> Hisobot davri</div>{role !== 'admin' && <button className="button button-outline" onClick={() => void onExport()}><Download size={15} /> CSV yuklab olish</button>}</div>
    <div className="stats-grid compact-stats">{cards.map((card) => <StatCard key={card.title} {...card} />)}</div>
    <div className="dashboard-main-grid report-grid report-grid-single"><section className="panel performance-panel"><div className="panel-heading"><div><div className="panel-kicker">HISOBOT</div><h2>{role === 'investor' ? 'Investitsiya holati' : role === 'beekeeper' ? 'Xo‘jalik davri' : 'Joriy ko‘rsatkichlar'}</h2><p>{role === 'beekeeper' && beekeeperReport ? `${formatDate(beekeeperReport.from)} — ${formatDate(beekeeperReport.to)}` : 'Jamlangan ma’lumotlar'}</p></div></div>
      {role === 'investor' && investorReport ? <div className="summary-strip"><div><span>Faol buyurtmalar qiymati</span><strong>{formatMoney(Number(investorReport.activePrincipalAmount))}</strong></div><div><span>Bekor qilingan buyurtmalar</span><strong>{investorReport.cancelledOrders}</strong></div><div><span>Faol investitsiyalar</span><strong>{investorReport.activeInvestments}</strong></div></div>
        : role === 'beekeeper' && beekeeperReport ? <div className="summary-strip"><div><span>Asalarixonalar</span><strong>{beekeeperReport.apiaryCount}</strong></div><div><span>Ari oilalari</span><strong>{beekeeperReport.colonyCount}</strong></div><div><span>Hisobot davri</span><strong>{formatDate(beekeeperReport.from)} — {formatDate(beekeeperReport.to)}</strong></div></div>
          : role === 'admin' && adminDashboard ? <div className="summary-strip"><div><span>Kutilayotgan buyurtmalar</span><strong>{adminDashboard.pendingOrders}</strong></div><div><span>To‘lovni tekshirish</span><strong>{adminDashboard.paymentReviewOrders}</strong></div><div><span>Yuborilgan hosil hisobotlari</span><strong>{adminDashboard.submittedHarvestReports}</strong></div></div>
            : <div className="empty-inline">Hisobot ma’lumotlari mavjud emas.</div>}
    </section></div>
    {role === 'beekeeper' && <section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">HISOBOT TARIXI</div><h2>Hosil qaydlari</h2></div></div>{harvests.length ? <HarvestTable harvests={harvests.slice(0, 20)} /> : <div className="empty-inline">Hali hosil qaydi kiritilmagan.</div>}</section>}
    {role === 'investor' && <section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">XO‘JALIK QAYDLARI</div><h2>Investor hisobotidagi hosil ma’lumotlari</h2></div></div>{investorReport?.apiaryReports.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>Qayd ID</th><th>Hosil sanasi</th><th>Miqdor</th><th>Holati</th></tr></thead><tbody>{investorReport.apiaryReports.map((row) => <tr key={row.id}><td className="table-primary">{row.id}</td><td className="muted-cell">{formatDate(row.harvestedOn)}</td><td className="strong-cell">{Number(row.quantityKg).toLocaleString('uz-UZ')} kg</td><td>{labelStatus(row.status)}</td></tr>)}</tbody></table></div> : <div className="empty-inline">Faol investitsiyalarga bog‘langan tasdiqlangan hosil qaydi hozircha yo‘q.</div>}</section>}
  </>;
}

function NotificationsPage({ notifications, onMarkAllRead, onRead }: { notifications: NotificationItem[]; onMarkAllRead: () => void | Promise<void>; onRead: (id: string) => void | Promise<void> }) {
  const sorted = [...notifications].sort((a, b) => Number(a.read) - Number(b.read));
  const IconMap: Record<NotificationItem['kind'], LucideIcon> = { payment: CircleDollarSign, report: FileText, listing: Sprout, system: Bell };
  return <>
    <PageHeading title="Bildirishnomalar" action="Barchasini o‘qildi" actionIcon={null} onAction={onMarkAllRead} />
    <div className="notification-list">{sorted.length ? sorted.map((item) => { const Icon = IconMap[item.kind]; return <article key={item.id} className={`notification-card ${!item.read ? 'unread' : ''}`}><span className={`notification-card-icon notify-${item.kind}`}><Icon size={18} /></span><div className="notification-copy"><strong>{item.title}</strong><p>{item.body}</p><time>{item.time}</time><span className={`notification-status ${item.read ? 'is-read' : 'is-unread'}`}><span className="notification-status-dot" />{item.read ? 'O‘qilgan' : 'O‘qilmagan'}</span></div>{!item.read && <button className="button button-outline notification-read" onClick={() => void onRead(item.id)}>O‘qilgan deb belgilash</button>}</article>; }) : <div className="empty-state"><Bell size={24} /><strong>Bildirishnoma yo‘q</strong><span>Yangi yangilanishlar shu yerda ko‘rsatiladi.</span></div>}</div>
  </>;
}

function ApiariesPage({ apiaries, onCreate, onEdit, onDeactivate }: { apiaries: Apiary[]; onCreate: () => void; onEdit: (item: Apiary) => void; onDeactivate: (item: Apiary) => void }) {
  return <>
    <PageHeading eyebrow="ASALARICHI KABINETI" title="Asalarixonalar" description="Xo‘jalik joylashuvi va qayd etilgan ari oilalari sonini boshqaring." action="Asalarixona qo‘shish" onAction={onCreate} />
    {apiaries.length ? <div className="apiary-grid">{apiaries.map((item, index) => <article className="apiary-card" key={item.id}><div className={`apiary-card-top apiary-tone-${index % 3}`}><img src="/apiary.jpg" alt="Asalarixona" /><span><MapPin size={13} />{item.place}</span></div><div className="apiary-card-body"><span className={`status-chip ${item.status === 'Faol' ? 'status-green' : 'status-honey'}`}><span />{item.status}</span><h3>{item.name}</h3><div className="apiary-card-stats"><div><strong>{item.colonies}</strong><span>Ari oilasi (uyalar soni)</span></div></div><div className="apiary-card-actions"><button className="text-link" onClick={() => void navigator.clipboard?.writeText(item.id)}>ID nusxalash <ArrowRight size={15} /></button><button className="button button-outline table-cancel" onClick={() => onEdit(item)}>Tahrirlash</button>{item.status === 'Faol' && <button className="button button-outline table-cancel" onClick={() => onDeactivate(item)}>Faol emas</button>}</div></div></article>)}</div> : <div className="empty-state"><Building2 size={25} /><strong>Asalarixona hali qo‘shilmagan</strong><span>Yangi asalarixona yaratib, keyin ari oilalarini va qaydlarni bog‘lang.</span><button className="button button-primary" onClick={onCreate}><Plus size={16} /> Asalarixona qo‘shish</button></div>}
  </>;
}

function ColoniesPage({ apiaries, colonies, onCreate, onEdit, onDeactivate }: { apiaries: Apiary[]; colonies: ColonyDto[]; onCreate: () => void; onEdit: (item: ColonyDto) => void; onDeactivate: (item: ColonyDto) => void }) {
  const apiaryNames = new Map(apiaries.map((item) => [item.id, item.name]));
  const total = colonies.reduce((sum, item) => sum + item.count, 0);
  const active = colonies.filter((item) => item.status === 'ACTIVE').reduce((sum, item) => sum + item.count, 0);
  return <>
    <PageHeading eyebrow="ASALARICHI KABINETI" title="Ari oilalari" description="Ari oilalarining soni, kuchi va asalarixona bog‘lanishini boshqaring." action="Oila qo‘shish" onAction={onCreate} />
    <div className="stats-grid compact-stats"><StatCard title="Jami uyalar" value={`${total} ta`} helper={`${colonies.length} ta qayd`} icon={Flower2} tone="green" /><StatCard title="Faol uyalar" value={`${active} ta`} helper="ACTIVE holatdagi yozuvlar" icon={CheckCircle2} tone="blue" /><StatCard title="Asalarixonalar" value={`${apiaries.length} ta`} helper="Asalarixonalar soni" icon={Building2} tone="honey" /></div>
    <section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">OILALAR REESTRI</div><h2>Barcha ari oilalari</h2></div></div>{colonies.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>Oila kodi</th><th>Asalarixona</th><th>Soni</th><th>Holati</th><th>Kuchi</th><th>Yaratilgan</th><th>Amal</th></tr></thead><tbody>{colonies.map((row) => <tr key={row.id}><td className="table-primary">{row.code || row.id}</td><td>{apiaryNames.get(row.apiaryId) || row.apiaryId}</td><td className="strong-cell">{row.count}</td><td><span className={`status-chip ${row.status === 'ACTIVE' ? 'status-green' : 'status-honey'}`}><span />{labelStatus(row.status)}</span></td><td>{row.strength || '—'}</td><td className="muted-cell">{formatDate(row.createdAt)}</td><td><div className="apiary-card-actions"><button className="button button-outline table-cancel" onClick={() => onEdit(row)}>Tahrirlash</button>{row.status === 'ACTIVE' && <button className="button button-outline table-cancel" onClick={() => onDeactivate(row)}>Faol emas</button>}</div></td></tr>)}</tbody></table></div> : <div className="empty-state"><Flower2 size={24} /><strong>Ari oilasi qayd etilmagan</strong><span>Asalarixona mavjud bo‘lsa, yangi ari oilasini qayd eting.</span></div>}</section>
  </>;
}

function HarvestsPage({ apiaries, harvests, expenses, tab, onTabChange, onAdd }: { apiaries: Apiary[]; harvests: HarvestRecord[]; expenses: ExpenseRecord[]; tab: 'harvest' | 'expense'; onTabChange: (tab: 'harvest' | 'expense') => void; onAdd: () => void }) {
  return <>
    <PageHeading eyebrow="ASALARICHI KABINETI" title="Hosil va xarajatlar" description="Ishlab chiqarish natijalari va xo‘jalik xarajatlarini ko‘ring." action={tab === 'harvest' ? 'Hosil qo‘shish' : 'Xarajat qo‘shish'} onAction={onAdd} />
    <div className="stats-grid compact-stats"><StatCard title="Jami hosil" value={`${harvests.reduce((sum, item) => sum + item.amount, 0).toLocaleString('uz-UZ')} kg`} helper="Qayd etilgan miqdor" icon={Droplets} tone="green" /><StatCard title="Jami xarajat" value={formatShortMoney(expenses.reduce((sum, item) => sum + item.amount, 0))} helper="Qayd etilgan summa" icon={CreditCard} tone="honey" /><StatCard title="Hisobotlar" value={`${harvests.length + expenses.length} ta`} helper="Hosil va xarajat qaydlari" icon={ClipboardList} tone="blue" /></div>
    {apiaries.length === 0 && <div className="notice-card"><CircleHelp size={19} /><div><strong>Avval asalarixona qo‘shing</strong><span>Hosil va xarajat yozuvlari asalarixonaga bog‘lanadi.</span></div></div>}
    <section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">XO‘JALIK QAYDLARI</div><h2>Hisobotlar ro‘yxati</h2></div><div className="segmented-control"><button className={tab === 'harvest' ? 'selected' : ''} onClick={() => onTabChange('harvest')}>Hosil</button><button className={tab === 'expense' ? 'selected' : ''} onClick={() => onTabChange('expense')}>Xarajatlar</button></div></div>{tab === 'harvest' ? harvests.length ? <HarvestTable harvests={harvests} /> : <div className="empty-inline">Hosil qaydi yo‘q.</div> : expenses.length ? <ExpenseTable expenses={expenses} /> : <div className="empty-inline">Xarajat qaydi yo‘q.</div>}</section>
  </>;
}

function HarvestReviewPage({ harvests, onReview }: { harvests: HarvestRecord[]; onReview: (id: string, status: 'APPROVED' | 'RETURNED') => void }) {
  const pending = harvests.filter((item) => item.status === 'Tekshiruvda' || item.status === 'Yuborildi' || item.status === 'SUBMITTED');
  return <><PageHeading eyebrow="ADMIN PANEL" title="Hosil moderatsiyasi" description="Asalarichilar yuborgan hosil qaydlarini tekshiring va tasdiqlang yoki tuzatishga qaytaring." /><div className="admin-summary"><div><span className="admin-summary-icon honey"><Clock3 size={18} /></span><div><small>Ko‘rib chiqish kutilmoqda</small><strong>{pending.length} ta hisobot</strong></div></div></div>{pending.length ? <div className="moderation-list">{pending.map((item) => <article className="moderation-card" key={item.id}><div className="moderation-main"><span className="status-chip status-honey"><span />{item.status}</span><h3>{item.place} · {item.amount.toLocaleString('uz-UZ')} kg</h3><p>{item.date}</p><div className="moderation-meta"><span>Hisobot ID: <strong>{item.id}</strong></span><span>Izoh: <strong>{item.note}</strong></span></div></div><div className="moderation-actions"><button className="button button-primary" onClick={() => onReview(item.id, 'APPROVED')}><Check size={15} /> Tasdiqlash</button><button className="button button-outline" onClick={() => onReview(item.id, 'RETURNED')}>Tuzatishga qaytarish</button></div></article>)}</div> : <div className="empty-state"><CheckCircle2 size={24} /><strong>Ko‘rib chiqilmagan hisobot yo‘q</strong><span>Yangi yuborilgan hisobotlar shu yerda ko‘rinadi.</span></div>}<section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">BARCHA QAYDLAR</div><h2>Hosil hisobotlari</h2></div></div>{harvests.length ? <HarvestTable harvests={harvests} /> : <div className="empty-inline">Hosil hisoboti mavjud emas.</div>}</section></>;
}

function SensorReadingsPage({ apiaries, colonies, readings, onCreate }: { apiaries: Apiary[]; colonies: ColonyDto[]; readings: SensorReadingDto[]; onCreate: () => void }) {
  const apiaryNames = new Map(apiaries.map((item) => [item.id, item.name]));
  const colonyCodes = new Map(colonies.map((item) => [item.id, item.code || item.id]));
  const sorted = [...readings].sort((a, b) => b.recordedAt.localeCompare(a.recordedAt));
  return <><PageHeading eyebrow="ASALARICHI KABINETI" title="Qo‘lda sensor qaydlari" description="IoT qurilmasi ulanmaganida harorat, namlik yoki uyalar vaznini qo‘lda qayd eting." action="O‘lchov kiritish" onAction={onCreate} /><div className="investment-disclaimer"><Thermometer size={17} /><div><strong>Qo‘lda kiritilgan o‘lchov</strong><span>Bu qaydlar qurilma telemetriyasi emas. Manba sifatida MANUAL belgilanadi; SIMULATED yoki avtomatik o‘lchov deb talqin qilinmaydi.</span></div></div>{sorted.length ? <section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">SENSOR QAYDLARI</div><h2>{sorted.length} ta qayd</h2></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>O‘lchangan vaqt</th><th>Asalarixona</th><th>Ari oilasi</th><th>Ko‘rsatkich</th><th>Qiymat</th><th>Manba</th></tr></thead><tbody>{sorted.map((item) => <tr key={item.id}><td className="muted-cell">{formatDate(item.recordedAt)}</td><td>{apiaryNames.get(item.apiaryId) || item.apiaryId}</td><td>{item.colonyId ? colonyCodes.get(item.colonyId) || item.colonyId : '—'}</td><td>{item.metric === 'TEMPERATURE' ? 'Harorat' : item.metric === 'HUMIDITY' ? 'Namlik' : 'Vazn'}</td><td className="strong-cell">{Number(item.value).toLocaleString('uz-UZ')} {item.unit}</td><td><span className="status-chip status-honey"><span />{item.source === 'MANUAL' ? 'Qo‘lda' : 'Simulyatsiya'}</span></td></tr>)}</tbody></table></div></section> : <div className="empty-state"><Thermometer size={24} /><strong>Sensor qaydi yo‘q</strong><span>O‘lchovni qo‘lda kiriting yoki keyinchalik tasdiqlangan IoT qurilmasini ulang.</span>{apiaries.length > 0 && <button className="button button-primary" onClick={onCreate}><Plus size={16} /> O‘lchov kiritish</button>}</div>}{apiaries.length === 0 && <div className="notice-card"><CircleHelp size={19} /><div><strong>Avval asalarixona yarating</strong><span>Sensor qaydi asalarixonaga bog‘lanishi kerak.</span></div></div>}</>;
}

function HarvestTable({ harvests }: { harvests: HarvestRecord[] }) {
  return <div className="table-scroll"><table className="data-table"><thead><tr><th>Hisobot ID</th><th>Asalarixona</th><th>Sana</th><th>Miqdor</th><th>Izoh</th><th>Holat</th></tr></thead><tbody>{harvests.map((item) => <tr key={item.id}><td className="table-primary">{item.id}</td><td>{item.place}</td><td className="muted-cell">{item.date}</td><td className="strong-cell">{item.amount.toLocaleString('uz-UZ')} kg</td><td className="muted-cell">{item.note}</td><td><span className={`status-chip ${item.status === 'Tasdiqlangan' ? 'status-green' : 'status-honey'}`}><span />{item.status}</span></td></tr>)}</tbody></table></div>;
}

function ExpenseTable({ expenses }: { expenses: ExpenseRecord[] }) {
  return <div className="table-scroll"><table className="data-table"><thead><tr><th>Qayd ID</th><th>Asalarixona</th><th>Sana</th><th>Turi</th><th>Summa</th><th>Holat</th></tr></thead><tbody>{expenses.map((item) => <tr key={item.id}><td className="table-primary">{item.id}</td><td>{item.place}</td><td className="muted-cell">{item.date}</td><td>{item.category}</td><td className="strong-cell">{formatMoney(item.amount)}</td><td><span className="status-chip status-green"><span />Qayd etildi</span></td></tr>)}</tbody></table></div>;
}

function SupportPage({ tickets, onSubmit }: { tickets: SupportTicketDto[]; onSubmit: (category: SupportTicketDto['category'], subject: string, message: string) => Promise<boolean> }) {
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const created = await onSubmit(String(data.get('category')) as SupportTicketDto['category'], String(data.get('subject') || ''), String(data.get('message') || ''));
    if (created) form.reset();
  };
  return <><PageHeading eyebrow="YORDAM MARKAZI" title="Yordam va murojaatlar" description="Savol, texnik muammo yoki shikoyatni yuboring. Murojaat shaxsiy kabinetda kuzatiladi." /><div className="support-layout"><form className="panel support-form" onSubmit={(event) => void submit(event)}><div className="panel-heading"><div><div className="panel-kicker">YANGI MUROJAAT</div><h2>Bizga yozing</h2></div><CircleHelp size={21} /></div><label>Murojaat turi<select name="category" defaultValue="GENERAL"><option value="GENERAL">Umumiy savol</option><option value="TECHNICAL">Texnik muammo</option><option value="COMPLAINT">Shikoyat</option></select></label><label>Mavzu<input name="subject" maxLength={180} required placeholder="Murojaat mavzusi" /></label><label>Batafsil ma’lumot<textarea name="message" maxLength={4000} rows={5} required placeholder="Muammoni yoki savolingizni tushuntiring" /></label><button className="button button-primary" type="submit"><ArrowRight size={15} /> Yuborish</button><small>Murojaat hisobingizga bog‘lanadi. To‘lov kartasi, parol yoki SMS kodni matnga kiritmang.</small></form><section className="panel support-list"><div className="panel-heading"><div><div className="panel-kicker">MUROJAATLARIM</div><h2>{tickets.length} ta yozuv</h2></div></div>{tickets.length ? tickets.map((ticket) => <article className="support-ticket" key={ticket.id}><div className="support-ticket-top"><span className="status-chip status-honey"><span />{supportCategory(ticket.category)}</span><span className={`status-chip ${ticket.status === 'RESOLVED' || ticket.status === 'CLOSED' ? 'status-green' : 'status-honey'}`}><span />{supportStatus(ticket.status)}</span></div><h3>{ticket.subject}</h3><p>{ticket.message}</p>{ticket.adminNote && <div className="support-admin-note"><strong>Javob / izoh</strong><span>{ticket.adminNote}</span></div>}<small>{formatDate(ticket.createdAt)}</small></article>) : <div className="empty-inline">Hozircha murojaat yuborilmagan.</div>}</section></div><div className="investment-disclaimer"><ShieldCheck size={16} /><div><strong>Yordam</strong><span>Murojaat yuborish operatorga real vaqtda jo‘natish kafolatini bermaydi. Admin paneldagi mas’ul xodim ko‘rib chiqadi; aloqa vaqtlari loyiha egasi tomonidan belgilanadi.</span></div></div></>;
}

function SupportAdminPage({ tickets, onReview }: { tickets: SupportTicketDto[]; onReview: (id: string, status: SupportTicketDto['status'], adminNote: string) => Promise<void> }) {
  const [statuses, setStatuses] = useState<Record<string, SupportTicketDto['status']>>({});
  const [notes, setNotes] = useState<Record<string, string>>({});
  return <><PageHeading eyebrow="ADMIN PANEL" title="Foydalanuvchi murojaatlari" description="Savollar, texnik muammolar va shikoyatlarni ko‘rib chiqing. Har bir holat audit jurnaliga yoziladi." /><div className="stats-grid compact-stats"><StatCard title="Jami yuklangan" value={`${tickets.length} ta`} helper="Joriy ro‘yxat" icon={CircleHelp} tone="green" /><StatCard title="Ochiq" value={`${tickets.filter((item) => item.status === 'OPEN').length} ta`} helper="Ko‘rib chiqilmagan" icon={Clock3} tone="honey" /><StatCard title="Ko‘rib chiqilmoqda" value={`${tickets.filter((item) => item.status === 'IN_REVIEW').length} ta`} helper="Administrator tomonidan ko‘rib chiqilmoqda" icon={ShieldCheck} tone="blue" /></div><div className="support-admin-list">{tickets.length ? tickets.map((ticket) => <article className="panel support-admin-card" key={ticket.id}><div className="support-ticket-top"><span className="status-chip status-honey"><span />{supportCategory(ticket.category)}</span><span className="status-chip status-green"><span />{supportStatus(ticket.status)}</span></div><h2>{ticket.subject}</h2><p>{ticket.message}</p><div className="support-admin-meta">Foydalanuvchi ID: <code>{ticket.userId}</code> · {formatDate(ticket.createdAt)}</div><div className="support-admin-controls"><label>Holat<select value={statuses[ticket.id] || ticket.status} onChange={(event) => setStatuses((prev) => ({ ...prev, [ticket.id]: event.target.value as SupportTicketDto['status'] }))}><option value="OPEN">Ochiq</option><option value="IN_REVIEW">Ko‘rib chiqilmoqda</option><option value="RESOLVED">Yechildi</option><option value="CLOSED">Yopildi</option></select></label><label>Ichki javob / izoh<textarea rows={2} maxLength={2000} value={notes[ticket.id] ?? ticket.adminNote} onChange={(event) => setNotes((prev) => ({ ...prev, [ticket.id]: event.target.value }))} placeholder="Foydalanuvchiga ko‘rinadigan javob" /></label><button className="button button-primary" onClick={() => void onReview(ticket.id, statuses[ticket.id] || ticket.status, notes[ticket.id] ?? ticket.adminNote)}>Saqlash</button></div></article>) : <div className="empty-state"><CircleHelp size={24} /><strong>Yangi murojaat yo‘q</strong><span>Yuborilgan murojaatlar shu yerda ko‘rinadi.</span></div>}</div></>;
}

function supportCategory(value: SupportTicketDto['category']) { return value === 'COMPLAINT' ? 'Shikoyat' : value === 'TECHNICAL' ? 'Texnik yordam' : 'Umumiy savol'; }
function supportStatus(value: SupportTicketDto['status']) { return value === 'IN_REVIEW' ? 'Ko‘rib chiqilmoqda' : value === 'RESOLVED' ? 'Yechilgan' : value === 'CLOSED' ? 'Yopilgan' : 'Ochiq'; }

function UsersPage({ users, currentUserId, canAssignAdmin, onStatus, onRoles }: { users: AdminUserDto[]; currentUserId: string; canAssignAdmin: boolean; onStatus: (id: string, status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED') => void; onRoles: (id: string, roles: string[]) => void }) {
  const [query, setQuery] = useState('');
  const [serverUsers, setServerUsers] = useState<AdminUserDto[] | null>(null);
  const [searchLoading, setSearchLoading] = useState(false);
  useEffect(() => {
    const term = query.trim();
    if (!term) { setServerUsers(null); setSearchLoading(false); return; }
    let alive = true;
    setServerUsers(null);
    setSearchLoading(true);
    const timer = window.setTimeout(() => {
      api.adminUsers(term, 0, 100).then((response) => {
        if (alive) setServerUsers(response.content);
      }).catch(() => {
        if (alive) setServerUsers(null);
      }).finally(() => {
        if (alive) setSearchLoading(false);
      });
    }, 250);
    return () => { alive = false; window.clearTimeout(timer); };
  }, [query]);
  const filtered = useMemo(() => {
    const term = normalizeSearchText(query);
    const localMatches = users.filter((user) => normalizeSearchText(`${user.fullName || ''} ${user.phone} ${user.email || ''} ${user.id} ${user.roles.join(' ')} ${user.accountStatus}`).includes(term));
    const unique = new Map<string, AdminUserDto>();
    [...localMatches, ...(serverUsers || [])].forEach((user) => unique.set(user.id, user));
    return [...unique.values()];
  }, [users, serverUsers, query]);
  const roleOptions = ['USER', 'INVESTOR', 'BEEKEEPER', 'ADMIN', 'SUPER_ADMIN'];
  const toggleRole = (user: AdminUserDto, role: string, checked: boolean) => {
    const next = checked ? [...new Set([...user.roles, role])] : user.roles.filter((item) => item !== role);
    onRoles(user.id, next);
  };
  return <><PageHeading eyebrow="ADMIN PANEL" title="Foydalanuvchilar" description="Foydalanuvchi rollari va hisob holatlarini boshqaring." /><div className="stats-grid compact-stats"><StatCard title="Yuklangan foydalanuvchi" value={`${users.length} ta`} helper="Joriy ro‘yxat" icon={Users} tone="green" /><StatCard title="Asalarichilar" value={`${users.filter((user) => user.roles.includes('BEEKEEPER')).length} ta`} helper="BEEKEEPER roli berilgan" icon={Sprout} tone="honey" /><StatCard title="To‘xtatilgan" value={`${users.filter((user) => user.accountStatus === 'SUSPENDED').length} ta`} helper="Hisob holati" icon={ShieldCheck} tone="blue" /></div><section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">FOYDALANUVCHILAR BAZASI</div><h2>{filtered.length} ta yuklangan foydalanuvchi</h2>{searchLoading && <span className="search-inline-loading"><LoaderCircle size={13} className="spin" /> Qidirilmoqda</span>}</div><label className="market-search"><Search size={15} /><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Ism, telefon yoki email" aria-label="Foydalanuvchini qidirish" /></label></div>{filtered.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>Foydalanuvchi</th><th>Kontakt</th><th>Rollar</th><th>Hisob holati</th><th>Ro‘yxatdan o‘tgan</th></tr></thead><tbody>{filtered.map((row) => { const isProtectedSuperAdmin = row.roles.includes('SUPER_ADMIN') && !canAssignAdmin; const isSelf = row.id === currentUserId; return <tr key={row.id}><td><div className="user-table-cell"><div className="avatar avatar-tiny">{initials(row.fullName || row.phone)}</div><span className="table-primary">{row.fullName || 'Ism kiritilmagan'}</span></div></td><td className="muted-cell">{row.phone}{row.email ? <div>{row.email}</div> : null}</td><td><div className="roles-control">{roleOptions.map((role) => { const isAdminRole = role === 'ADMIN' || role === 'SUPER_ADMIN'; const disabled = isProtectedSuperAdmin || (isAdminRole && !canAssignAdmin) || (isSelf && (role === 'ADMIN' || role === 'SUPER_ADMIN') && row.roles.includes(role)); return <label key={role} title={disabled ? 'Bu rolni faqat Super Admin boshqaradi yoki o‘z admin rolingizni olib tashlay olmaysiz.' : undefined}><input type="checkbox" checked={row.roles.includes(role)} disabled={disabled} onChange={(event) => toggleRole(row, role, event.target.checked)} />{role === 'SUPER_ADMIN' ? 'Super Admin' : ROLE_LABELS[viewRole(role) || 'investor']}</label>; })}</div></td><td><select aria-label={`${row.phone} hisob holati`} value={row.accountStatus} disabled={isProtectedSuperAdmin || isSelf} onChange={(event) => onStatus(row.id, event.target.value as 'ACTIVE' | 'SUSPENDED' | 'CLOSED')}><option value="ACTIVE">Faol</option><option value="SUSPENDED">To‘xtatilgan</option><option value="CLOSED">Yopilgan</option><option value="PENDING" disabled>Kutilmoqda</option></select><span className={`status-chip ${row.accountStatus === 'ACTIVE' ? 'status-green' : 'status-honey'}`}><span />{labelStatus(row.accountStatus)}</span></td><td className="muted-cell">{formatDate(row.createdAt)}</td></tr>; })}</tbody></table></div> : <div className="empty-inline">Mos foydalanuvchi topilmadi.</div>}</section><div className="investment-disclaimer"><ShieldCheck size={16} /><div><strong>Rol xavfsizligi</strong><span>Admin va Super Admin huquqlarini faqat Super Admin tayinlay oladi. O‘zingizning yoki yagona Super Admin hisobining bloklanishi taqiqlanadi.</span></div></div></>;
}

function InvestorPaymentsPage({ payments }: { payments: PaymentDto[] }) {
  return <><PageHeading eyebrow="INVESTOR KABINETI" title="To‘lovlarim" description="Hisobingizga bog‘langan to‘lov yozuvlari." /><div className="stats-grid compact-stats"><StatCard title="To‘lov yozuvlari" value={`${payments.length} ta`} helper="Joriy ro‘yxat" icon={CreditCard} tone="green" /><StatCard title="Tasdiqlangan to‘lovlar" value={`${payments.filter((item) => item.status === 'SUCCESS').length} ta`} helper="Tasdiqlangan to‘lovlar" icon={CheckCircle2} tone="blue" /><StatCard title="Kutilayotgan" value={`${payments.filter((item) => item.status === 'PENDING' || item.status === 'PAYMENT_REVIEW').length} ta`} helper="Yakuniy bo‘lmagan holat" icon={Clock3} tone="honey" /></div>{payments.length ? <section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">TO‘LOV TARIXI</div><h2>Tranzaksiya yozuvlari</h2></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>To‘lov ID</th><th>Buyurtma</th><th>To‘lov usuli</th><th>Summa</th><th>Holat</th><th>Sana</th></tr></thead><tbody>{payments.map((item) => <tr key={item.id}><td className="table-primary">{item.id}</td><td>{item.investmentId}</td><td>{item.provider}</td><td className="strong-cell">{item.currency === 'UZS' ? formatMoney(Number(item.amount)) : `${item.amount} ${item.currency}`}</td><td><span className={`status-chip ${item.status === 'SUCCESS' ? 'status-green' : 'status-honey'}`}><span />{labelStatus(item.status)}</span></td><td className="muted-cell">{formatDate(item.paidAt || item.createdAt)}</td></tr>)}</tbody></table></div></section> : <div className="empty-state"><CreditCard size={24} /><strong>To‘lov yozuvi mavjud emas</strong><span>Tranzaksiya yozuvlari paydo bo‘lgach shu yerda ko‘rinadi.</span></div>}</>;
}

function PaymentsPage({ payments }: { payments: PaymentDto[] }) {
  return <><PageHeading eyebrow="ADMIN PANEL" title="To‘lovlar" description="To‘lov yozuvlari va tranzaksiya holatlari." /><div className="stats-grid compact-stats"><StatCard title="Yuklangan yozuvlar" value={`${payments.length} ta`} helper="Joriy ro‘yxat" icon={CreditCard} tone="green" /><StatCard title="Tasdiqlangan" value={`${payments.filter((item) => item.status === 'SUCCEEDED' || item.status === 'SUCCESS').length} ta`} helper="Muvaffaqiyatli to‘lovlar" icon={CheckCircle2} tone="blue" /><StatCard title="Ko‘rib chiqilmoqda" value={`${payments.filter((item) => item.status === 'PENDING' || item.status === 'REVIEW').length} ta`} helper="Yakuniy holat kutilmoqda" icon={Clock3} tone="honey" /></div><section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">TRANZAKSIYA YOZUVLARI</div><h2>To‘lovlar</h2></div></div>{payments.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>To‘lov ID</th><th>Buyurtma ID</th><th>Summa</th><th>To‘lov usuli</th><th>Holat</th><th>Sana</th><th>Tranzaksiya raqami</th></tr></thead><tbody>{payments.map((row) => <tr key={row.id}><td className="table-primary">{row.id}</td><td>{row.investmentId}</td><td className="strong-cell">{row.currency === 'UZS' ? formatMoney(Number(row.amount)) : `${row.amount} ${row.currency}`}</td><td>{row.provider}</td><td><span className={`status-chip ${row.status === 'SUCCEEDED' || row.status === 'SUCCESS' ? 'status-green' : 'status-honey'}`}><span />{labelStatus(row.status)}</span></td><td className="muted-cell">{formatDate(row.paidAt || row.createdAt)}</td><td className="muted-cell">{row.providerTransactionId || '—'}</td></tr>)}</tbody></table></div> : <div className="empty-inline">To‘lov yozuvi yo‘q.</div>}</section></>;
}

function AuditPage({ rows }: { rows: AuditLogDto[] }) {
  return <><PageHeading eyebrow="ADMIN PANEL" title="Audit jurnali" description="Ma’muriy amallar va natijalar tarixi." /><section className="panel full-table-panel"><div className="panel-heading"><div><div className="panel-kicker">AUDIT</div><h2>{rows.length} ta qayd</h2></div></div>{rows.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>Vaqt</th><th>Actor</th><th>Amal</th><th>Entity</th><th>Natija</th><th>Tafsilot</th></tr></thead><tbody>{rows.map((row) => <tr key={row.id}><td className="muted-cell">{formatDate(row.createdAt)}</td><td>{row.actorId || 'System'}</td><td className="table-primary">{row.action}</td><td>{row.entityType}{row.entityId ? ` · ${row.entityId}` : ''}</td><td><span className={`status-chip ${row.outcome === 'SUCCESS' ? 'status-green' : 'status-honey'}`}><span />{row.outcome}</span></td><td className="audit-details">{JSON.stringify(row.details)}</td></tr>)}</tbody></table></div> : <div className="empty-inline">Audit yozuvi hozircha yo‘q.</div>}</section></>;
}

function SettingsPage({ preferences, onSave }: { preferences: NotificationPreferencesDto | null; onSave: (value: Omit<NotificationPreferencesDto, 'updatedAt'>) => void }) {
  const [draft, setDraft] = useState({ investmentUpdates: true, reportUpdates: true, marketingUpdates: false, pushEnabled: false });
  useEffect(() => { if (preferences) setDraft({ investmentUpdates: preferences.investmentUpdates, reportUpdates: preferences.reportUpdates, marketingUpdates: preferences.marketingUpdates, pushEnabled: preferences.pushEnabled }); }, [preferences]);
  const entries: [keyof typeof draft, string, string][] = [
    ['investmentUpdates', 'Buyurtma va investitsiya yangiliklari', 'Buyurtma holati va investitsiyaga tegishli xabarlar'],
    ['reportUpdates', 'Hisobot yangiliklari', 'Hosil va xarajatlar bo‘yicha bildirishnomalar'],
    ['marketingUpdates', 'Platforma e’lonlari', 'Mahsulot yangiliklari va tanishtiruv xabarlari'],
    ['pushEnabled', 'Push bildirishnomalar', 'Push bildirishnomalarini olishni boshqaring.'],
  ];
  return <><PageHeading eyebrow="SOZLAMALAR" title="Bildirishnoma sozlamalari" description="O‘zingizga kerakli bildirishnomalarni tanlang." /><section className="panel settings-panel"><div className="settings-section-title"><div className="settings-icon"><Bell size={18} /></div><div><h2>Qabul qilinadigan bildirishnomalar</h2><p>{preferences ? `Oxirgi yangilanish: ${formatDate(preferences.updatedAt)}` : 'Sozlamalar hozircha mavjud emas.'}</p></div></div>{entries.map(([key, title, help]) => <label className="settings-row" key={key}><div><strong>{title}</strong><span>{help}</span></div><input type="checkbox" checked={draft[key]} onChange={(event) => setDraft((current) => ({ ...current, [key]: event.target.checked }))} /></label>)}<button className="button button-primary" disabled={!preferences} onClick={() => onSave(draft)}>Sozlamalarni saqlash <Check size={15} /></button></section></>;
}

const MAX_PROFILE_IMAGE_FILE_BYTES = 5 * 1024 * 1024;
const MAX_PROFILE_IMAGE_DATA_LENGTH = 350_000;

function prepareProfileImage(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const supportedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!supportedTypes.includes(file.type)) {
      reject(new Error('JPG, PNG yoki WebP formatidagi rasm tanlang.'));
      return;
    }
    if (file.size > MAX_PROFILE_IMAGE_FILE_BYTES) {
      reject(new Error('Rasm hajmi 5 MB dan oshmasligi kerak.'));
      return;
    }

    const source = URL.createObjectURL(file);
    const image = new Image();
    image.onerror = () => { URL.revokeObjectURL(source); reject(new Error('Rasmni ochib bo‘lmadi. Boshqa fayl tanlang.')); };
    image.onload = () => {
      try {
        const cropSize = Math.min(image.naturalWidth, image.naturalHeight);
        if (!cropSize || image.naturalWidth > 8192 || image.naturalHeight > 8192) throw new Error('Rasm o‘lchami juda katta. Kichikroq rasm tanlang.');
        const cropX = (image.naturalWidth - cropSize) / 2;
        const cropY = (image.naturalHeight - cropSize) / 2;
        for (const side of [512, 384, 320]) {
          const canvas = document.createElement('canvas');
          canvas.width = side;
          canvas.height = side;
          const context = canvas.getContext('2d');
          if (!context) throw new Error('Rasmni tayyorlab bo‘lmadi.');
          context.fillStyle = '#ffffff';
          context.fillRect(0, 0, side, side);
          context.drawImage(image, cropX, cropY, cropSize, cropSize, 0, 0, side, side);
          for (const quality of [0.84, 0.74, 0.64, 0.54]) {
            const dataUrl = canvas.toDataURL('image/jpeg', quality);
            if (dataUrl.length <= MAX_PROFILE_IMAGE_DATA_LENGTH) {
              resolve(dataUrl);
              return;
            }
          }
        }
        reject(new Error('Rasmni siqib bo‘lmadi. Boshqa, kichikroq rasm tanlang.'));
      } catch (error) {
        reject(error instanceof Error ? error : new Error('Rasmni tayyorlab bo‘lmadi.'));
      } finally {
        URL.revokeObjectURL(source);
      }
    };
    image.src = source;
  });
}

function ProfilePage({ user, role, onSave }: { user: UserProfile; role: Role; onSave: (fullName: string, email: string, profileImageData: string) => Promise<void> }) {
  const [fullName, setFullName] = useState(user.fullName || '');
  const [email, setEmail] = useState(user.email || '');
  const [profileImageData, setProfileImageData] = useState(user.profileImageData || '');
  const [imageBusy, setImageBusy] = useState(false);
  const [imageError, setImageError] = useState('');
  const [saving, setSaving] = useState(false);
  const photoInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setFullName(user.fullName || '');
    setEmail(user.email || '');
    setProfileImageData(user.profileImageData || '');
  }, [user.id, user.fullName, user.email, user.profileImageData]);

  const profileName = fullName.trim() || 'Ism kiritilmagan';
  const accountActive = user.accountStatus === 'ACTIVE';
  const accountStatus = labelStatus(user.accountStatus);

  const changePhoto = async (input: HTMLInputElement) => {
    const file = input.files?.[0];
    if (!file) return;
    setImageError('');
    setImageBusy(true);
    try {
      setProfileImageData(await prepareProfileImage(file));
    } catch (issue) {
      setImageError(issue instanceof Error ? issue.message : 'Rasmni tayyorlab bo‘lmadi.');
    } finally {
      setImageBusy(false);
      input.value = '';
    }
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    try { await onSave(fullName.trim(), email.trim(), profileImageData); }
    finally { setSaving(false); }
  };

  return (
    <>
      <PageHeading eyebrow="SHAXSIY KABINET" title="Profil" description="Shaxsiy ma’lumotlar va hisob xavfsizligini boshqaring." />
      <div className="profile-layout">
        <section className="panel profile-card">
          <div className="profile-cover" aria-hidden="true">
            {profileImageData && <img className="profile-cover-image" src={profileImageData} alt="" />}
            <div className="profile-cover-tint" />
          </div>
          <form className="profile-main" onSubmit={(event) => void submit(event)}>
            <div className="profile-identity">
              <div className="profile-avatar-wrap">
                <div className="profile-avatar">
                  {profileImageData ? <img src={profileImageData} alt="Profil rasmi" /> : <UserRound size={34} strokeWidth={1.7} />}
                </div>
                <input ref={photoInput} className="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp" aria-label="Profil rasmi fayli" onChange={(event) => void changePhoto(event.currentTarget)} />
              </div>
              <div className="profile-identity-copy">
                <h2>{profileName}</h2>
                <span className="profile-role">{ROLE_LABELS[role]}</span>
              </div>
              <span className={`profile-account-pill ${accountActive ? 'is-active' : 'is-inactive'}`}>
                <span className="profile-account-dot" />{accountStatus}
              </span>
            </div>

            <div className="profile-photo-controls">
              <button className="button button-outline profile-photo-button" type="button" onClick={() => photoInput.current?.click()} disabled={imageBusy || saving}>
                <Camera size={15} />{imageBusy ? 'Rasm tayyorlanmoqda…' : profileImageData ? 'Boshqa rasm tanlash' : 'Profil rasmi qo‘shish'}
              </button>
              {profileImageData && <button className="profile-photo-remove" type="button" onClick={() => { setProfileImageData(''); setImageError(''); }} disabled={saving}><Trash2 size={14} /> Olib tashlash</button>}
              <span className="profile-photo-help">JPG, PNG yoki WebP · 5 MB gacha</span>
            </div>
            {imageError && <div className="profile-image-error" role="alert">{imageError}</div>}

            <div className="profile-details" aria-label="Hisob ma’lumotlari">
              <div className="profile-detail"><span>Telefon raqam</span><strong>{user.phone}</strong></div>
              <div className="profile-detail"><span>Ro‘yxatdan o‘tgan</span><strong>{formatDate(user.createdAt)}</strong></div>
            </div>

            <div className="profile-edit-section">
              <div className="profile-edit-heading">
                <div><h3>Shaxsiy ma’lumotlar</h3><p>Ism familiya va email manzilingizni tahrirlang.</p></div>
              </div>
              <div className="profile-edit-grid">
                <label className="profile-field">Ism familiya
                  <input name="fullName" autoComplete="name" value={fullName} onChange={(event) => setFullName(event.target.value)} maxLength={160} placeholder="Ism familiyangiz" required />
                </label>
                <label className="profile-field">Elektron pochta
                  <input name="email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} maxLength={254} placeholder="name@example.com" />
                </label>
              </div>
              <div className="profile-edit-actions">
                <button className="button button-primary profile-save" type="submit" disabled={saving || imageBusy}>
                  {saving ? <LoaderCircle size={16} className="spin" /> : <Check size={16} />}
                  {saving ? 'Saqlanmoqda…' : 'O‘zgarishlarni saqlash'}
                </button>
              </div>
            </div>
          </form>
        </section>

        <aside className="panel profile-security">
          <div className="profile-security-header">
            <div>
              <div className="panel-kicker">HISOB XAVFSIZLIGI</div>
              <h2>Kirish va tasdiqlash</h2>
              <p>Hisobingizga kirish usuli.</p>
            </div>
            <span className="profile-security-icon"><ShieldCheck size={20} /></span>
          </div>
          <div className="profile-security-list">
            <div className="security-row">
              <span className="security-icon"><Phone size={18} /></span>
              <div><strong>Telefon raqami</strong><span>{user.phone}</span></div>
              <span className="security-tag"><CheckCircle2 size={14} /> Tasdiqlangan</span>
            </div>
            <div className="security-row">
              <span className="security-icon"><ShieldCheck size={18} /></span>
              <div><strong>Kirish usuli</strong><span>Bir martalik SMS kodi (OTP)</span></div>
            </div>
          </div>
          <div className="profile-security-note">
            <CircleHelp size={17} />
            <div><strong>Kodni hech kimga bermang</strong><span>Hisobingizdan chiqish uchun menyudagi chiqish tugmasidan foydalaning.</span></div>
          </div>
        </aside>
      </div>
    </>
  );
}

function PublicPage({ listings, totalListings, onBrowse, onDetail, onContact }: { listings: Listing[]; totalListings: number; onBrowse: () => void; onDetail: (item: Listing) => void; onContact: () => void }) {
  return <>
    <div className="public-hero"><div className="public-hero-copy"><span className="hero-badge"><Leaf size={13} /> ASALARICHILIK EKOTIZIMI</span><h1>Tabiat ne’matini<br /><em>yangi imkoniyatga</em> aylantiring.</h1><p>MELARIUM asalarichilik xo‘jaliklari va investorlar uchun yagona raqamli platforma.</p><div className="public-hero-actions"><button className="button button-primary" onClick={onBrowse}>Faol takliflarni ko‘rish <ArrowRight size={16} /></button><button className="button button-white" onClick={onBrowse}>Platforma imkoniyatlari</button></div><div className="public-trust"><span><ShieldCheck size={15} /> Telefon orqali tasdiqlash</span><span><Users size={15} /> Investor va asalarichi kabinetlari</span></div></div><div className="public-hero-image"><img src="/apiary.jpg" alt="Asalarichilik xo‘jaligi" /><div className="public-image-card"><span className="public-image-icon"><Sprout size={17} /></span><div><strong>Asalarichilik ekotizimi</strong><span>Web va mobil platforma</span></div><CheckCircle2 size={17} /></div></div></div>
    <div className="public-metrics"><div><strong>{totalListings}</strong><span>Faol takliflar</span></div><div><strong>{listings.length}</strong><span>Hozir ko‘rsatilgan takliflar</span></div><div><strong>UZS</strong><span>Asosiy valyuta</span></div><div><strong>OTP</strong><span>Telefon orqali kirish</span></div></div>
    <section className="public-listings"><div className="panel-heading"><div><div className="panel-kicker">FAOL TAKLIFLAR</div><h2>Asalarichilik takliflari</h2><p>Taklif tafsilotlari va hozirgi mavjudligini ko‘ring.</p></div><button className="text-link" onClick={onBrowse}>Bozorga o‘tish <ArrowRight size={15} /></button></div>{listings.length ? <div className="recommendation-row">{listings.slice(0, 3).map((item) => <MiniListingCard key={item.id} item={item} onClick={() => onDetail(item)} />)}</div> : <div className="empty-inline">Hozircha faol taklif yo‘q.</div>}</section>
    <section className="public-faq"><div className="public-faq-heading"><div><div className="panel-kicker">SAVOL-JAVOB</div><h2>Ko‘p so‘raladigan savollar</h2></div><button className="button button-outline" onClick={onContact}>Yordam va aloqa <ArrowRight size={15} /></button></div><details><summary>Ro‘yxatdan qanday o‘tiladi?</summary><p>Kabinetga telefon raqami va bir martalik tasdiqlash kodi (OTP) orqali kirasiz. Kirish kodi yoki refresh tokenni boshqa odamga bermang.</p></details><details><summary>Yordam yoki shikoyatni qayerga yuboraman?</summary><p>Hisobga kirib, “Yordam va murojaatlar” bo‘limidan umumiy savol, texnik muammo yoki shikoyat yuboring. Bu yerda aloqa telefoni/email ko‘rsatilmayapti, chunki operator ma’lumotlari MELARIUM egasi tomonidan tasdiqlanmagan.</p><button className="text-link" onClick={onContact}>Murojaat yuborish <ArrowRight size={15} /></button></details></section>
  </>;
}

function todayInput() {
  const today = new Date();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${today.getFullYear()}-${month}-${day}`;
}

function Modal({ kind, listing, apiary, colony, apiaries, colonies, canInvest, onClose, onSubmit, onOpenInvest }: ModalProps) {
  const [quantity, setQuantity] = useState(1);
  const [apiaryId, setApiaryId] = useState(apiaries[0]?.id || '');
  useEffect(() => {
    setQuantity(1);
    setApiaryId(listing && apiaries.some((item) => item.id === listing.apiaryId) ? listing.apiaryId : colony?.apiaryId || apiaries[0]?.id || '');
  }, [kind, listing?.id, colony?.id, apiaries]);
  if (!kind) return null;
  const title = kind === 'details' ? 'Taklif tafsilotlari'
    : kind === 'invest' ? 'Buyurtma yaratish'
      : kind === 'harvest' ? 'Hosil hisobotini qo‘shish'
        : kind === 'expense' ? 'Xarajatni qayd etish'
          : kind === 'apiary' ? apiary ? 'Asalarixonani tahrirlash' : 'Asalarixona qo‘shish'
            : kind === 'colony' ? colony ? 'Ari oilasini tahrirlash' : 'Ari oilasini qo‘shish' : kind === 'sensor' ? 'Sensor o‘lchovini kiritish' : kind === 'listing' && listing ? 'Taklifni tahrirlash' : 'Yangi taklif yaratish';
  const selectedColonies = colonies.filter((item) => item.apiaryId === apiaryId);
  const apiarySelect = <label>Asalarixona<select name="apiaryId" value={apiaryId} onChange={(event) => setApiaryId(event.target.value)} required><option value="" disabled>Asalarixonani tanlang</option>{apiaries.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.place}</option>)}</select></label>;
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div className="modal-header"><div><div className="panel-kicker">MELARIUM</div><h2 id="modal-title">{title}</h2></div><button className="icon-plain" onClick={onClose} aria-label="Yopish"><X size={20} /></button></div>
        {kind === 'details' && listing ? (
          <div className="detail-modal-body">
            <div className="detail-modal-image"><img src="/apiary.jpg" alt="Asalarixona va ari uyalari" /><span className="verified-tag"><Sprout size={13} /> Faol taklif</span></div>
            <div className="detail-modal-location"><MapPin size={14} /> {listing.place}</div>
            <h3>{listing.name}</h3>
            <div className="detail-modal-owner"><div className="avatar avatar-tiny">{initials(listing.beekeeper)}</div><span>Taklif egasi</span><strong>{listing.beekeeper}</strong></div>
            <p className="detail-modal-description">{listing.description}</p>
            <div className="detail-modal-stats"><div><span>Narxi / birlik</span><strong>{formatMoney(listing.price)}</strong></div><div><span>Mavjud</span><strong>{listing.units} ta</strong></div><div><span>Taklif turi</span><strong>{listing.category}</strong></div></div>
            <div className="modal-actions"><button className="button button-outline" onClick={onClose}>Yopish</button>{canInvest && <button className="button button-primary" onClick={onOpenInvest}>Buyurtma berish <ArrowRight size={15} /></button>}</div>
          </div>
        ) : (
          <form className="modal-form" onSubmit={onSubmit}>
            {kind === 'invest' && listing && <>
              <div className="modal-listing-summary"><img src="/apiary.jpg" alt="Asalarixona" /><div><strong>{listing.name}</strong><span><MapPin size={12} /> {listing.place}</span><small>{formatMoney(listing.price)} / birlik</small></div></div>
              <label>Birliklar soni<input name="quantity" type="number" min="1" max={Math.max(listing.units, 1)} value={quantity} onChange={(event) => setQuantity(Math.min(Math.max(1, Number(event.target.value) || 1), Math.max(listing.units, 1)))} required /></label>
              <div className="modal-total"><span>Jami qiymat</span><strong>{formatMoney(listing.price * quantity)}</strong></div>
            </>}
            {kind === 'harvest' && <>{apiarySelect}{selectedColonies.length > 0 && <label>Ari oilasi (ixtiyoriy)<select name="colonyId" defaultValue=""><option value="">Ko‘rsatilmagan</option>{selectedColonies.map((item) => <option key={item.id} value={item.id}>{item.code || item.id} · {item.count} ta</option>)}</select></label>}<div className="form-row"><label>Hosil sanasi<input name="date" type="date" max={todayInput()} defaultValue={todayInput()} required /></label><label>Miqdor (kg)<input name="amount" type="number" min="0.001" step="0.001" placeholder="0" required /></label></div><label>Izoh<textarea name="note" rows={3} maxLength={4000} placeholder="Hosil bo‘yicha izoh" /></label></>}
            {kind === 'expense' && <>{apiarySelect}{selectedColonies.length > 0 && <label>Ari oilasi (ixtiyoriy)<select name="colonyId" defaultValue=""><option value="">Ko‘rsatilmagan</option>{selectedColonies.map((item) => <option key={item.id} value={item.id}>{item.code || item.id} · {item.count} ta</option>)}</select></label>}<div className="form-row"><label>Xarajat sanasi<input name="date" type="date" max={todayInput()} defaultValue={todayInput()} required /></label><label>Summa (so‘m)<input name="amount" type="number" min="0.01" step="0.01" placeholder="0" required /></label></div><label>Xarajat turi<select name="category" required><option value="FEED">Ozuqa va parvarish</option><option value="VETERINARY">Veterinariya</option><option value="EQUIPMENT">Uskuna</option><option value="TRANSPORT">Transport</option><option value="OTHER">Boshqa xarajat</option></select></label><label>Izoh<textarea name="note" rows={3} maxLength={4000} placeholder="Xarajat bo‘yicha izoh" /></label></>}
            {kind === 'sensor' && <>{apiarySelect}{selectedColonies.length > 0 && <label>Ari oilasi (ixtiyoriy)<select name="colonyId" defaultValue=""><option value="">Ko‘rsatilmagan</option>{selectedColonies.map((item) => <option key={item.id} value={item.id}>{item.code || item.id} · {item.count} ta</option>)}</select></label>}<label>O‘lchov turi<select name="metric" required><option value="TEMPERATURE">Harorat (°C)</option><option value="HUMIDITY">Namlik (%)</option><option value="WEIGHT">Uya vazni (kg)</option></select></label><label>O‘lchangan qiymat<input name="value" type="number" min="-50" max="1000" step="0.1" required /></label><div className="form-warning"><Thermometer size={15} /> Manba MANUAL sifatida qayd etiladi. Tizim o‘lchovni IoT qurilmasidan olingan deb belgilamaydi.</div></>}
            {kind === 'apiary' && <><label>Asalarixona nomi<input name="name" maxLength={160} placeholder="Masalan: Bahor bog‘i" defaultValue={apiary?.name || ''} required /></label><label>Joylashuv<input name="place" maxLength={240} placeholder="Viloyat, tuman" defaultValue={apiary?.place || ''} required /></label><label>Tavsif<textarea name="description" rows={3} maxLength={4000} placeholder="Xo‘jalik haqida qisqacha" defaultValue={apiary?.description || ''} /></label></>}
            {kind === 'colony' && <>{colony ? <label>Asalarixona<input value={apiaries.find((item) => item.id === colony.apiaryId)?.name || colony.apiaryId} readOnly /></label> : apiarySelect}<label>Oila kodi (ixtiyoriy)<input name="code" maxLength={40} placeholder="Masalan: AR-001" defaultValue={colony?.code || ''} /></label><div className="form-row"><label>Uyalar soni<input name="count" type="number" min="1" max="500" defaultValue={colony?.count || 1} required /></label><label>Kuchi (ixtiyoriy)<select name="strength" defaultValue={colony?.strength || ''}><option value="">Ko‘rsatilmagan</option><option value="WEAK">Kuchsiz</option><option value="MEDIUM">O‘rtacha</option><option value="STRONG">Kuchli</option></select></label></div><label>Izoh<textarea name="note" rows={3} maxLength={4000} placeholder="Ari oilasi haqida" defaultValue={colony?.description || ''} /></label></>}
            {kind === 'listing' && <>{apiarySelect}<label>Taklif nomi<input name="name" maxLength={180} placeholder="Taklif nomi" defaultValue={listing?.name || ''} required /></label><label>Joylashuv<input name="place" maxLength={240} placeholder="Viloyat, tuman" defaultValue={listing?.place || ''} required /></label><div className="form-row"><label>Narx / birlik (so‘m)<input name="price" type="number" min="0.01" step="0.01" placeholder="0" defaultValue={listing?.price || ''} required /></label><label>Jami birliklar soni<input name="units" type="number" min="1" max="10000" placeholder="1" defaultValue={listing?.unitsTotal || listing?.units || ''} required /></label></div><label>Taklif tavsifi<textarea name="description" rows={3} maxLength={4000} placeholder="Xo‘jalik va taklif haqida batafsil ma’lumot" defaultValue={listing?.description || ''} required /></label><div className="form-warning"><ShieldCheck size={15} /> {listing ? 'Tahrir saqlangach, taklif yana moderatsiyaga yuboriladi.' : 'Yangi taklif admin moderatsiyasidan o‘tadi va tasdiqlanmaguncha bozorda ko‘rinmaydi.'}</div></>}
            {apiaries.length === 0 && ['harvest', 'expense', 'colony', 'listing', 'sensor'].includes(kind) && <div className="form-warning"><CircleHelp size={15} /> Avval faol asalarixona yarating. Bu amal uchun asalarixona kerak.</div>}
            <div className="modal-actions"><button type="button" className="button button-outline" onClick={onClose}>Bekor qilish</button><button className="button button-primary" type="submit" disabled={['harvest', 'expense', 'colony', 'listing', 'sensor'].includes(kind) && apiaries.length === 0}>{kind === 'invest' ? 'Buyurtma yaratish' : kind === 'listing' ? 'Moderatsiyaga yuborish' : 'Saqlash'} <ArrowRight size={15} /></button></div>
          </form>
        )}
      </section>
    </div>
  );
}
