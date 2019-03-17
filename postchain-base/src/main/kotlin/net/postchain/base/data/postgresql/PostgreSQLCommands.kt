package net.postchain.base.data.postgresql

import net.postchain.base.data.SQLCommands

object PostgreSQLCommands : SQLCommands {
    override val createTableBlocks: String = "CREATE TABLE blocks" +
                                            " (block_iid BIGSERIAL PRIMARY KEY," +
                                            "  block_height BIGINT NOT NULL, " +
                                            "  block_rid BYTEA," +
                                            "  chain_id BIGINT NOT NULL," +
                                            "  block_header_data BYTEA," +
                                            "  block_witness BYTEA," +
                                            "  timestamp BIGINT," +
                                            "  UNIQUE (chain_id, block_rid)," +
                                            "  UNIQUE (chain_id, block_height))"


}