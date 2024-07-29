-- Create "user_profile" table
CREATE TABLE `user_profile` (`user_id` text NOT NULL, `user_name` text NOT NULL, PRIMARY KEY (`user_id`)) STRICT;
-- Create "post" table
CREATE TABLE `post` (`id` integer NULL PRIMARY KEY AUTOINCREMENT, `parent_id` integer NULL, `author_id` text NOT NULL, `content` text NOT NULL, `createdAt` integer NOT NULL DEFAULT (unixepoch('subsec')*1000), CONSTRAINT `0` FOREIGN KEY (`author_id`) REFERENCES `user_profile` (`user_id`) ON UPDATE NO ACTION ON DELETE NO ACTION) STRICT;
-- Create "vote_event" table
CREATE TABLE `vote_event` (`vote_event_id` integer NOT NULL PRIMARY KEY AUTOINCREMENT, `user_id` text NOT NULL, `post_id` integer NOT NULL, `vote` integer NOT NULL, `vote_event_time` integer NOT NULL DEFAULT (unixepoch('subsec')*1000), `parent_id` integer NULL, CONSTRAINT `0` FOREIGN KEY (`user_id`) REFERENCES `user_profile` (`user_id`) ON UPDATE NO ACTION ON DELETE NO ACTION) STRICT;
-- Create "vote" table
CREATE TABLE `vote` (`user_id` text NOT NULL, `post_id` integer NOT NULL, `vote` integer NOT NULL, `latest_vote_event_id` integer NOT NULL, `vote_event_time` integer NOT NULL, PRIMARY KEY (`user_id`, `post_id`), CONSTRAINT `0` FOREIGN KEY (`user_id`) REFERENCES `user_profile` (`user_id`) ON UPDATE NO ACTION ON DELETE NO ACTION) STRICT;

CREATE TRIGGER after_insert_on_vote_event after insert on vote_event
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
