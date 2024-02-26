package pub.dtm.client.barrier.itfc.impl;

import org.apache.commons.lang3.StringUtils;
import pub.dtm.client.barrier.itfc.BarrierDBOperator;
import pub.dtm.client.constant.ParamFieldConstants;
import pub.dtm.client.enums.TransTypeEnum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;

/**
 * BarrierDBOperator for MySQL
 *
 * @author horseLk
 * @date 2022-09-28 22:05
 */
public class BarrierMysqlOperator implements BarrierDBOperator {
    private Object connection;

    public BarrierMysqlOperator(Object connection) {
        this.connection = connection;
    }

    @Override
    public boolean insertBarrier(String transType, String gid, String branchId, String op, int barrierId) throws Exception {
        if (Objects.isNull(connection)) {
            return false;
        }
        TransTypeEnum transTypeEnum = TransTypeEnum.parseString(transType);
        if (Objects.isNull(transTypeEnum)) {
            return false;
        }
        return insertBarrier(transTypeEnum, gid, branchId, op, barrierId);
    }

    @Override
    public boolean insertBarrier(TransTypeEnum transType, String gid, String branchId, String op, int barrierId) throws Exception {
        if (Objects.isNull(connection)) {
            return false;
        }
        if (Objects.isNull(transType)) {
            return false;
        }
        String tryOperator = transType.getTryOperator();
        String cancelOperator = transType.getCancelOperator();
        if (StringUtils.isEmpty(tryOperator) || StringUtils.isEmpty(cancelOperator)){
            return false;
        }
        Connection conn = (Connection)this.connection;
        conn.setAutoCommit(false);
        PreparedStatement preparedStatement = null;
        try {
            String sql = "insert ignore into barrier(trans_type, gid, branch_id, op, barrier_id, reason) values(?,?,?,?,?,?)";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, transType.getValue());
            preparedStatement.setString(2, gid);
            preparedStatement.setString(3, branchId);
            preparedStatement.setString(4, op);
            preparedStatement.setString(5, String.format("%02d", barrierId));
            preparedStatement.setString(6, op);

            if (preparedStatement.executeUpdate() == 0) {
                return false;
            }
            if (cancelOperator.equals(op)) {
                int opIndex = 4;
                preparedStatement.setString(opIndex, tryOperator);
                if (preparedStatement.executeUpdate() > 0) {
                    return false;
                }
            }
        } finally {
            if (Objects.nonNull(preparedStatement)) {
                preparedStatement.close();
            }
        }
        return true;
    }

    @Override
    public void commit() throws Exception {
        if (Objects.isNull(connection)) {
            return;
        }
        Connection conn = (Connection)this.connection;
        conn.commit();
        conn.setAutoCommit(true);
    }

    @Override
    public void rollback() throws Exception {
        if (Objects.isNull(connection)) {
            return;
        }
        Connection conn = (Connection)this.connection;
        conn.rollback();
        conn.setAutoCommit(true);
    }
}
