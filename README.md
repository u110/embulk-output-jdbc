# JDBC output plugins for Embulk

JDBC output plugins for Embulk loads records to databases using JDBC drivers.

## MySQL

See [embulk-output-mysql](embulk-output-mysql/).

## PostgreSQL

See [embulk-output-postgresql](embulk-output-postgresql/).

## Oracle

See [embulk-output-oracle](embulk-output-oracle/).

## Redshift

See [embulk-output-redshift](embulk-output-redshift/).

## Generic

### Overview

* **Plugin type**: output
* **Load all or nothing**: depnds on the mode:
  * **insert**: no
  * **replace**: yes
* **Resume supported**: no

### Configuration

- **driver_path**: path to the jar file of the JDBC driver (e.g. 'sqlite-jdbc-3.8.7.jar') (string, optional)
- **driver_class**: class name of the JDBC driver (e.g. 'org.sqlite.JDBC') (string, required)
- **url**: URL of the JDBC connection (e.g. 'jdbc:sqlite:mydb.sqlite3') (string, required)
- **user**: database login user name (string, optional)
- **password**: database login password (string, optional)
- **schema**: destination schema name (string, default: use default schema)
- **table**: destination table name (string, required)
- **mode**: "insert", "insert_direct", "truncate_insert", or "replace". See bellow (string, required)
- **batch_size**: size of a single batch insert (integer, default: 16777216)
- **options**: extra JDBC properties (hash, default: {})
- **timestamp_format**: strftime(3) format when embulk writes a timestamp value to a VARCHAR or CLOB column (string, default: `%Y-%m-%d %H:%M:%S.%6N`)
- **timezone**: timezone used to format a timestamp value using `timestamp_format` (string, default: UTC)
- **max_table_name_length**: maximum length of table name in this RDBMS (integer, default: 256)

### Modes

* **insert**:
  * Behavior: This mode writes rows to some intermediate tables first. If all those tasks run correctly, runs `INSERT INTO <target_table> SELECT * FROM <intermediate_table_1> UNION ALL SELECT * FROM <intermediate_table_2> UNION ALL ...` query.
  * Transactional: Yes. This mode successfully writes all rows, or fails with writing zero rows.
  * Resumable: Yes.
* **insert_direct**:
  * Behavior: This mode inserts rows to the target table directly.
  * Transactional: No. If fails, the target table could have some rows inserted.
  * Resumable: No.
* **truncate_insert**:
  * Behavior: Same with `insert` mode excepting that it truncates the target table right before the lst `INSERT ...` query.
  * Transactional: Yes.
  * Resumable: Yes.
* **replace**:
  * Behavior: Same with `insert` mode excepting that it truncates the target table right before the lst `INSERT ...` query.
  * Transactional: Yes.
  * Resumable: No.

### Example

```yaml
out:
  type: jdbc
  driver_path: /opt/oracle/ojdbc6.jar
  driver_class: oracle.jdbc.driver.OracleDriver
  url: jdbc:oracle:thin:@127.0.0.1:1521:mydb
  user: myuser
  password: "mypassword"
  table: my_table
  mode: insert
```

### Build

```
$ ./gradlew gem
```
