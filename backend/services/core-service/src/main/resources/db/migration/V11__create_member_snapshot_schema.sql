CREATE TABLE public.member_snapshots (
    member_id bigint NOT NULL,
    name character varying(50) NOT NULL,
    phone character varying(50) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT member_snapshots_pkey PRIMARY KEY (member_id)
);
