CREATE TABLE "${pref}ft_assets" (
  chain_iid BIGINT NOT NULL,
  asset_iid SERIAL PRIMARY KEY,
  asset_id TEXT NOT NULL
);

CREATE TABLE "${pref}ft_accounts" (
    account_iid BIGSERIAL PRIMARY KEY,
    tx_iid BIGINT NOT NULL REFERENCES "${pref}transactions"(tx_iid),
    op_index INT NOT NULL,
    chain_iid BIGINT NOT NULL,
    account_id BYTEA NOT NULL,
    account_type INT NOT NULL,
    account_desc BYTEA NOT NULL,
    UNIQUE (chain_iid, account_id)
);

CREATE TABLE "${pref}ft_balances" (
    account_iid BIGINT NOT NULL REFERENCES "${pref}ft_accounts"(account_iid),
    asset_iid BIGINT NOT NULL REFERENCES "${pref}ft_assets"(asset_iid),
    balance BIGINT NOT NULL,
    PRIMARY KEY (account_iid, asset_iid)
);

CREATE TABLE "${pref}ft_history" (
    tx_iid BIGINT NOT NULL ,
    op_index INT NOT NULL,
    account_iid BIGINT NOT NULL REFERENCES "${pref}ft_accounts"(account_iid),
    asset_iid BIGINT NOT NULL REFERENCES "${pref}ft_assets"(asset_iid),
    delta BIGINT NOT NULL,
    memo TEXT
);


CREATE FUNCTION "${pref}ft_register_account"(chain_iid BIGINT, tx_iid BIGINT, op_index INT, account_id BYTEA, account_type INT, account_desc BYTEA)
RETURNS VOID AS $$
BEGIN
    INSERT INTO "${pref}ft_accounts"(chain_iid, tx_iid, op_index, account_id, account_type, account_desc)
    VALUES (chain_iid, tx_iid, op_index, account_id, account_type, account_desc);
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION "${pref}ft_register_asset"(chain_iid BIGINT, asset_id TEXT)
RETURNS VOID AS $$
BEGIN
    INSERT INTO "${pref}ft_assets"(chain_iid, asset_id)
    VALUES (chain_iid, asset_id);
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION "${pref}ft_find_account"(chain_iid_ BIGINT, account_id_ BYTEA)
RETURNS BIGINT AS $$
BEGIN
  RETURN (SELECT account_iid FROM "${pref}ft_accounts" WHERE
    chain_iid = chain_iid_ AND account_id = account_id_);
 END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION "${pref}ft_get_account_desc" (chain_iid_ BIGINT, account_id_ BYTEA)
RETURNS BYTEA AS $$
BEGIN
    RETURN (SELECT account_desc FROM "${pref}ft_accounts" WHERE
        chain_iid = chain_iid_ AND account_id = account_id_);
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION "${pref}ft_find_asset"(chain_iid_ BIGINT, asset_id_ TEXT)
RETURNS BIGINT AS $$
BEGIN
  RETURN (SELECT asset_iid FROM "${pref}ft_assets" WHERE
   chain_iid = chain_iid_ AND asset_id = asset_id_);
END;
$$ LANGUAGE  plpgsql;

CREATE FUNCTION "${pref}ft_update"
(chain_iid BIGINT, tx_iid BIGINT, op_index INT, account_id BYTEA, asset_id TEXT, delta BIGINT,
 memo TEXT, allowNegative BOOLEAN)
RETURNS VOID AS $$
DECLARE
  account_iid_ BIGINT;
  asset_iid_ BIGINT;
  balance_ BIGINT;
BEGIN
  asset_iid_ = "${pref}ft_find_asset"(chain_iid, asset_id);
  account_iid_ = "${pref}ft_find_account"(chain_iid, account_id);
  SELECT balance INTO balance_
    FROM "${pref}ft_balances" WHERE "${pref}ft_balances".account_iid = account_iid_ AND "${pref}ft_balances".asset_iid = asset_iid_;

  IF (delta < 0) AND NOT allowNegative THEN
    IF (balance_ is NULL) OR (balance_ + delta) < 0 THEN
      RAISE EXCEPTION 'Insufficient balance';
    END IF;
  END IF;

  IF balance_ IS NULL THEN
    INSERT INTO "${pref}ft_balances"(account_iid, asset_iid, balance) VALUES (account_iid_, asset_iid_, delta);
  ELSE
    UPDATE "${pref}ft_balances" SET balance = balance + delta
    WHERE ("${pref}ft_balances".account_iid = account_iid_) AND ("${pref}ft_balances".asset_iid = asset_iid_);
  END IF;

  INSERT INTO "${pref}ft_history" (tx_iid, op_index, account_iid, asset_iid, delta, memo)
    VALUES (tx_iid, op_index, account_iid_, asset_iid_, delta, memo);
END;
$$ LANGUAGE plpgsql;

--- Queries

CREATE FUNCTION "${pref}ft_get_balance" (chain_iid BIGINT, account_id BYTEA, asset_id TEXT)
RETURNS BIGINT AS $$
DECLARE
  account_iid_ BIGINT;
  asset_iid_ BIGINT;
  balance_ BIGINT;
BEGIN
  asset_iid_ = "${pref}ft_find_asset"(chain_iid, asset_id);
  account_iid_ = "${pref}ft_find_account"(chain_iid, account_id);
  SELECT balance INTO balance_
    FROM "${pref}ft_balances" WHERE "${pref}ft_balances".account_iid = account_iid_ AND "${pref}ft_balances".asset_iid = asset_iid_;

  IF balance_ is NULL THEN
    balance_ = 0;
   END IF;

   RETURN balance_;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION "${pref}ft_get_history"(chain_iid BIGINT, account_id BYTEA, asset_id TEXT)
RETURNS TABLE (
    delta BIGINT,
    tx_rid BYTEA,
    op_index INT,
    memo TEXT,
    block_header_data BYTEA
) AS $$
DECLARE
  account_iid_ BIGINT;
  asset_iid_ BIGINT;
BEGIN
  asset_iid_ = "${pref}ft_find_asset"(chain_iid, asset_id);
  account_iid_ = "${pref}ft_find_account"(chain_iid, account_id);

  RETURN QUERY SELECT
     "${pref}ft_history".delta,
     "${pref}transactions".tx_rid,
     "${pref}ft_history".op_index,
     "${pref}ft_history".memo,
     "${pref}blocks".block_header_data
  FROM "${pref}ft_history"
  INNER JOIN "${pref}transactions" ON "${pref}ft_history".tx_iid = "${pref}transactions".tx_iid
  INNER JOIN "${pref}blocks" ON "${pref}transactions".block_iid = "${pref}blocks".block_iid
  WHERE account_iid = account_iid_ AND asset_iid = asset_iid_
  ORDER BY "${pref}ft_history".tx_iid;

END;
$$ LANGUAGE plpgsql;