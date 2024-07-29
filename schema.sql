create table user_profile(
  user_id text not null primary key,
  user_name text not null
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
