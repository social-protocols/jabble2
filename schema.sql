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
