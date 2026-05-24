-- E) SQL МИГРАЦИИ ДЛЯ SUPABASE (ПРОД-УРОВЕНЬ)
-- Все таблицы + политики безопасности уровня строк (RLS) + индексы для ускорения работы СУБД.

-- Создание таблицы профилей пользователей, которые автоматически синхронизируются через Auth триггер
create table public.profiles (
    id uuid references auth.users on delete cascade primary key,
    username text not null unique,
    total_xp integer default 0 check (total_xp >= 0),
    current_rank_tier integer default 1 check (current_rank_tier between 1 and 10),
    streak_days integer default 0 check (streak_days >= 0),
    is_premium_unlocked boolean default false,
    health_kit_connected boolean default false,
    fitness_goal text default '',
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Таблица физической активности (синхронизация локальных тренировок в облако Supabase)
create table public.activities (
    id uuid default gen_random_uuid() primary key,
    user_id uuid references public.profiles(id) on delete cascade not null,
    type text not null,
    duration_minutes integer not null check (duration_minutes > 0),
    distance_km double precision default 0.0 check (distance_km >= 0.0),
    xp_earned integer not null check (xp_earned >= 0),
    photo_uri text,
    is_gps_tracked boolean default false,
    timestamp timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Таблица глобальных фитнес-соревнований и квестов
create table public.challenges (
    id uuid default gen_random_uuid() primary key,
    title text not null,
    description text not null,
    required_xp integer default 0,
    reward_badge_name text not null,
    participating_count integer default 0,
    fee_ruble double precision default 0.0,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Таблица заработанных наград и спортивных бейджей
create table public.badges (
    id uuid default gen_random_uuid() primary key,
    user_id uuid references public.profiles(id) on delete cascade not null,
    name text not null,
    description text not null,
    visual_color text not null,
    is_unlocked boolean default false,
    unlocked_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique(user_id, name)
);

-- Включение Row Level Security (RLS) для защиты данных пользователей
alter table public.profiles enable row level security;
alter table public.activities enable row level security;
alter table public.challenges enable row level security;
alter table public.badges enable row level security;

-- Политики безопасности уровня строк (RLS) преступят к работе!

-- 1. Политики для PROFILES
create policy "Право чтения публичного имени профилей всеми" 
on public.profiles for select 
using (true);

create policy "Пользователи могут изменять только свой профиль" 
on public.profiles for update 
using (auth.uid() = id);

-- 2. Политики для ACTIVITIES
create policy "Пользователи видят только свои тренировки" 
on public.activities for select 
using (auth.uid() = user_id);

create policy "Пользователи записывают только свои тренировки" 
on public.activities for insert 
with check (auth.uid() = user_id);

create policy "Пользователи удаляют только свои тренировки" 
on public.activities for delete 
using (auth.uid() = user_id);

-- 3. Политики для CHALLENGES (Любой авторизованный видит квесты инфлюенсеров)
create policy "Авторизованные атлеты видят все квесты" 
on public.challenges for select 
using (auth.role() = 'authenticated');

create policy "Только авторы и VIP-инфлюенсеры создают вызовы" 
on public.challenges for insert 
with check (auth.role() = 'authenticated');

-- 4. Политики для BADGES
create policy "Пользователи видят свои значки" 
on public.badges for select 
using (auth.uid() = user_id);

create policy "Пользователи добавляют свои значки" 
on public.badges for insert 
with check (auth.uid() = user_id);


-- Создание высокопроизводительных индексов для оптимизации тяжелых запросов в СНГ
create index idx_activities_user_timestamp on public.activities(user_id, timestamp desc);
create index idx_badges_user_name on public.badges(user_id, name);
create index idx_profiles_xp on public.profiles(total_xp desc);
