-- https://docs.sqlc.dev/en/stable/reference/query-annotations.html

-- name: getReplyIds :many
select id
from post
where parent_id = ?;

-- name: getDescendantIds :many
select descendant_id
from lineage
where ancestor_id = ?;

-- name: getVote :many
select vote
from vote
where user_id = ?
and post_id = ?;

-- name: getVoteCount :one
select count(*)
from vote
where post_id = ?
and vote != 0;
