create table user_profile(
  user_id text not null primary key
  , user_name text not null
  -- created_at integer not null default (unixepoch('subsec')*1000) -- TODO
  , is_admin integer not null default false
) strict;

create table post(
  id integer primary key autoincrement -- rowid
  , parent_id integer
  , author_id text not null
  , content text not null
  , created_at integer not null default (unixepoch('subsec')*1000)
  , deleted_at integer default null
  , is_private integer not null default false
  , constraint post_parent_id_fkey foreign key (parent_id) references post(id) on delete no action on update no action
  , constraint post_author_id_fkey foreign key (author_id) references user_profile(user_id) on delete no action on update no action
) strict;

create table lineage(
  ancestor_id integer
  , descendant_id integer not null
  , separation integer not null
  , primary key(ancestor_id, descendant_id)
) strict;
create index lineage_ancestor_id on lineage(ancestor_id);
create index lineage_descendant_id on lineage(descendant_id);

create trigger after_insert_post after insert on post when new.parent_id is not null
begin
    -- Insert a lineage record for parent
    insert into lineage(ancestor_id, descendant_id, separation)
    values(new.parent_id, new.id, 1) on conflict do nothing;

    -- Insert a lineage record for all ancestors of this parent
    insert into lineage
    select
        ancestor_id
        , new.id as descendant_id
        , 1 + separation as separation
    from lineage ancestor
    where ancestor.descendant_id = new.parent_id;
end;

create table vote_event(
  vote_event_id integer not null primary key autoincrement
  , user_id text not null references user_profile(user_id)
  , post_id integer not null
  , vote integer not null
  , vote_event_time integer not null default (unixepoch('subsec')*1000)
  , parent_id integer
) strict;
create index vote_event_user_id_post_id_idx on vote_event(user_id, post_id);

create table vote (
  user_id text references user_profile(user_id)
  , post_id integer not null
  , vote integer not null
  , latest_vote_event_id integer not null
  , vote_event_time integer not null
  , primary key(user_id, post_id)
) strict;

create trigger after_insert_on_vote_event after insert on vote_event
begin
  insert into vote(
    user_id
    , post_id
    , vote
    , latest_vote_event_id
    , vote_event_time
  ) values (
    new.user_id
    , new.post_id
    , new.vote
    , new.vote_event_id
    , new.vote_event_time
  ) on conflict(user_id, post_id) do update set
    vote = new.vote
    , latest_vote_event_id = new.vote_event_id
    , vote_event_time = new.vote_event_time;
end;

create table effect_event(
  vote_event_id integer not null
  , vote_event_time integer not null
  , post_id integer not null
  , comment_id integer not null
  , p real not null
  , p_count integer not null
  , p_size integer not null
  , q real not null
  , q_count integer not null
  , q_size integer not null
  , r real not null
  , weight real not null default 0
  , primary key(vote_event_id, post_id, comment_id)
) strict;

create table effect(
  vote_event_id integer not null
  , vote_event_time integer not null
  , post_id integer not null
  , comment_id integer not null
  , p real not null
  , p_count integer not null
  , p_size integer not null
  , q real not null
  , q_count integer not null
  , q_size integer not null
  , r real not null
  , weight real not null default 0
  , primary key(post_id, comment_id)
) strict;

create trigger after_insert_effect_event after insert on effect_event
begin
  insert or replace into effect
  values (
    new.vote_event_id
    , new.vote_event_time
    , new.post_id
    , new.comment_id
    , new.p
    , new.p_count
    , new.p_size
    , new.q
    , new.q_count
    , new.q_size
    , new.r
    , new.weight
  );
end;

create table score_event(
  vote_event_id integer not null
  , vote_event_time integer not null
  , post_id integer not null
  , o real not null
  , o_count integer not null
  , o_size integer not null
  , p real not null
  , score real not null
  , primary key(vote_event_id, post_id)
) strict;

create table score(
  vote_event_id integer not null
  , vote_event_time integer not null
  , post_id integer not null
  , o real not null
  , o_count integer not null
  , o_size integer not null
  , p real not null
  , score real not null
  , primary key(post_id)
) strict;

create trigger after_insert_on_score_event after insert on score_event
begin
  insert or replace into score
  values(
    new.vote_event_id
    , new.vote_event_time
    , new.post_id
    , new.o
    , new.o_count
    , new.o_size
    , new.p
    , new.score
  );
end;

create view score_with_default as
select
    post.id as post_id
    , ifnull(o,0.5) o
    , ifnull(o_count,0) o_count
    , ifnull(o_size,0) o_size
    , ifnull(score.p, 0.5) p
    , ifnull(score,0) score
from post
left join score
on post.id = score.post_id;

create view effect_with_default as
select
   ancestor_id as post_id
   , descendant_id as comment_id
   , s.p
   , coalesce(effect.p_count, s.o_count, 0) p_count
   , coalesce(effect.p_size, s.o_count, 0) p_size
   , coalesce(effect.q, s.o, 0.5) q
   , coalesce(effect.q_count, s.o_count, 0.5) q_count
   , coalesce(effect.q_size, s.o_size, 0.5) q_size
   , coalesce(effect.r, s.o, 0.5) r
from score_with_default s
join lineage
  on ancestor_id = s.post_id
left join effect
  on effect.comment_id = ancestor_id
  and effect.post_id = descendant_id;
