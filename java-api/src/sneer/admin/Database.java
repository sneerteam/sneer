package sneer.admin;

import java.util.List;
import java.util.Map;

public interface Database {

	void createTable(String tableName, List<List<Object>> columns);

	void createIndex(String tableName, String indexName, List<String> columns, boolean unique);

	/**
	 *
	 * @param tableName
	 * @param row
	 * @return the row id of the newly inserted row
	 */
	long insert(String tableName, Map<String, Object> row) throws UniqueConstraintViolated;

	/**
	 * The first row contains the name of the columns in the result set.
	 *
	 * @param sql
	 * @param params
	 * @return
	 */
	Iterable<List<?>> query(String sql, List<Object> params);

}