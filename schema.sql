create table user_profile(
  user_id text not null primary key,
  user_name text not null
) strict;

create table post(
  id integer primary key autoincrement -- rowid
  , parent_id integer
  , author_id text not null references user_profile(user_id)
  , content text not null
  , createdAt integer not null default (unixepoch('subsec')*1000)
) strict;

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
