# gsheet-jdbc

> Connect to any Google Spreadsheet with JDBC

## Table of Contents

- [Background](#background)
- [Overview](#how-it-works)
- [Usage](#usage)
- [JDBC URL Format](#jdbc-url-format)
- [Authentication](#authentication)
- [Table Schemas](#database-table-schemas)
- [SQL Queries](#sql-queries)
- [Contribute](#contribute)
- [License](#license)

## Background

gsheet-jdbc is a Java JDBC driver that turns any Google Spreadsheet into a read-only database.

## How It Works

When a client program creates an intitial JDBC connection, gsheet-jdbc will download one or more sheets contained within a Google spreadsheet document, [infer the database schema](#database-table-schemas) (column names and types), and copy the data into an [H2 inmemory database](https://www.h2database.com/html/main.html).  H2 provides the heavy lifting for handling subsequent SQL queries.

Subsequent connections will continue to leverage the existing database unless the contents become stale (detected by querying the last modification datestamp of the spreadsheet document). 

By copying the data into an in-memory database, the driver reduces the risk of running into any rate limits on the Google Sheet or Drive APIs.  Most connections will incur minimal delay as the data is already cached locally.   THe primary downside is the cost of refreshing the data on the first connection or whenever the data becomes stale.  This process maintains an exclusive lock on the Driver to ensure consistency (at the cost of extra latency when these events occur).

## Usage

Install the following package:

TBD.

## JDBC URL Format

The JDBC URL encoding allows developers to specify one or multiple sheets and corresponding properties including:
1. The Google document ID
2. The sheet name and range of cells to load

In addition, the URL can specify the database schema to place the tables under.

The format is:

```
jdbc:gsheet://doc=(id={{GOOGLE_DOCUMENT_ID1}},range={{SHEET_NAME_AND_RANGE}}),doc=(id={{GOOGLE_DOCUMENT_ID2,range={{SHEET_NAME_AND_RANGE}})/{{DATABASE_SCHEMA}}
```

Both the document ID and range formats are provided by Google.  A valid range consists of:
1. A sheet name 
2. Followed by a `!` separator character 
3. Followed by the column and number of the upper left cell
4. Followed by a `:` separator character
5. Followed by the column and number of the lower right cell

For example, the following URL loads two sheets:

```
jdbc:gsheet://doc=(id=abcdefg,range=Sheet1!A1:G11),doc=(id=xyz123,range=Sheet2!A1:G11)/MySchema
```
The first sheet has
1. A document ID `abcdefg`,
2. A sheet name `Sheet1`
3. A cell range from `A1` to `G11`

Both sheets are loaded as `Sheet1` & `Sheet2` tables into the database schema `MySchema`.

## Authentication

The driver looks for an environment variable, `GSHEET_JDBC_CREDENTIALS`, containing the contents of a Google service account credentials JSON file.

The service account must be setup with `VIEW ACCESS` in a project with both the google sheet and google drive APIs enabled.  The Google drive API is required to fetch document timestamps to determine data freshness.

Each document must be shared with the IAM email address of the service account so the driver can read the file contents.

## Database Table Schemas

The first row of the range provided in the JDBC URL must contain the column headers or names for each column.  These names will be mapped to physical columns names in the H2 database.  Column names are restricted
to alphanumeric (ASCII) characters, space, hyphen, and underscore.  

Column types are derived from the format (explicit or implied) of cells in the first data row.  Supported column types include:
- **Text** - The default format if none is applied.  It maps to `VARCHAR` in the database.
- **Number** - For numeric, non-date columns.  It maps to `DOUBLE` in the database.
- **Date** - For calendar days.  It maps to `DATE` in the database.
- **Datetime** - For calendar days plus time to milliseconds.  It maps to `TIMESTAMP` in the database.
- **Boolean** - For true/false columns.  It maps to `BOOLEAN` in the database.

No foreign keys, primary keys, or indices are created.

## SQL Queries

Make sure you double quote (`"`) the schema, table, and column names in your queries.  The table must be prefixed by the schema name in SQL queries:

```sql
Select * from \"MySchema\".\"Class Data\" WHERE \"Student Name\"='Alexandra' LIMIT 1;
```

Queries leverage the [H2 database](https://github.com/h2database/h2database) dialect.

## Contribute
Please refer to [the contributing.md file](CONTRIBUTING.md) for information about how to get involved. We welcome issues, questions, and pull requests.

Community chat is now on [discord](https://discord.gg/ApvtW5YU)

## License

This project is licensed under the [MIT License](LICENSE.md).
