create table user_profile(
  user_id text not null primary key,
  user_name text not null
) strict;

create table foo(
  id integer primary key -- rowid
) strict;

