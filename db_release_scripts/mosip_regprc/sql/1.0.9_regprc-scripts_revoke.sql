-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.0.9
-- Purpose    		: Revoking Database Alter deployement done for release in Registration ProcessorDB.       
-- Create By   		: Sadanandegowda DM
-- Created Date		: 27-Apr-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

ALTER TABLE regprc.abis_response_det ADD COLUMN score numeric(6,3) NOT NULL;

----------------------------------------------------------------------------------------------------