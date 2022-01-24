

-- object: regprc.anonymous_profile | type: TABLE --
-- DROP TABLE IF EXISTS regprc.anonymous_profile CASCADE;
CREATE TABLE regprc.anonymous_profile(
	id character varying(39) NOT NULL,
    	process_stage character varying(36) NOT NULL,
    	profile character varying NOT NULL,
    	cr_by character varying(256) NOT NULL,
    	cr_dtimes timestamp NOT NULL,
    	upd_by character varying(256),
    	upd_dtimes timestamp,
    	is_deleted boolean DEFAULT FALSE,
    	del_dtimes timestamp,
    	CONSTRAINT pk_anonymous_id PRIMARY KEY (id)
);
-- ddl-end --

