\c mosip_regprc 
TRUNCATE TABLE regprc.transaction_type cascade ;
\COPY regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes) FROM './dml/regprc-transaction_type.csv' delimiter ',' HEADER  csv;
