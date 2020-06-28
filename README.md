# tv-submit project

```
# delete topic if you need to reset data (restart materaliaze container as well)
/opt/kafka_2.12-2.2.0/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic tripvibe
```

TimelyDataFlow using materialize.io

```
# schema load
psql -h localhost -p 6875 materialize -f ./load.sql
psql -h localhost -p 6875 materialize -f ./load-auto.sql

# psql -h localhost -p 6875 materialize

# create kafka source
CREATE SOURCE tripvibe
FROM KAFKA BROKER 'localhost:9092' TOPIC 'tripvibe'
FORMAT TEXT;

SHOW COLUMNS FROM tripvibe;

# materialize all data
CREATE MATERIALIZED VIEW all_tripvibe AS
    SELECT (text::JSONB)->'location_lng' as location_lng,
           (text::JSONB)->'location_lat' as location_lat,
           CAST((text::JSONB)->'timestamp_created' AS timestamptz) as timestamp_created,
           CAST((text::JSONB)->'sentiment'->'departure_time' AS timestamptz) as departure_time,           
           (text::JSONB)->'sentiment'->'vibe' as vibe,
           (text::JSONB)->'sentiment'->'capacity' as capacity,
           (text::JSONB)->'sentiment'->'route_direction' as route_direction,
           (text::JSONB)->'sentiment'->'route_type' as route_type,
           (text::JSONB)->'sentiment'->'route_number' as route_number,
           (text::JSONB)->'sentiment'->'stop_name' as stop_name,
           (text::JSONB)->'submitter'->'device_id' as device_id
    FROM (SELECT * FROM tripvibe);

SELECT * from all_tripvibe;
SHOW COLUMNS FROM all_tripvibe;

CREATE SOURCE ptv_all_routes
FROM FILE '/work/tv-submit-data/all_routes.json'
FORMAT REGEX '^(?P<data>\{.*)$';

SHOW COLUMNS FROM ptv_all_routes;

CREATE MATERIALIZED VIEW all_routes AS
    SELECT CAST((data::JSONB)->'route_type' as int) as route_type,
           CAST((data::JSONB)->'route_id' as int) as route_id,
           (data::JSONB)->'route_name' as route_name,
           (data::JSONB)->'route_number' as route_number
    FROM ptv_all_routes;


#CREATE MATERIALIZED VIEW all_routes AS
#  SELECT data::jsonb AS values FROM ptv_all_routes;

select * from all_routes LIMIT 10;

DROP VIEW all_routes;
DROP SOURCE ptv_all_routes;

#

SELECT avg(capacity) from ROUTE216
WHERE route_direction = 'foo'

SELECT avg(capacity) from ROUTE216
WHERE stop_name = 'foo'

SELECT avg(capacity) from ROUTE216
WHERE departure_time between start and end;

# materalize only the 216 bus
CREATE MATERIALIZED VIEW ROUTE216 AS
    SELECT CAST(location_lng AS float), CAST(location_lat AS float), CAST(vibe AS integer), CAST(capacity AS integer), route_direction, route_type, route_number, timestamp_created, departure_time, stop_name, device_id  
    FROM all_tripvibe
    WHERE route_number = '"216"';

SELECT * from ROUTE216;
SHOW COLUMNS FROM ROUTE216;

#
CREATE MATERIALIZED VIEW ROUTE90 AS
    SELECT CAST(location_lng AS float), CAST(location_lat AS float), CAST(vibe AS integer), CAST(capacity AS integer), route_direction, route_type, route_number, timestamp_created, departure_time, stop_name, device_id  
    FROM all_tripvibe
    WHERE route_number = '"90"';

SELECT * from ROUTE90;
SHOW COLUMNS FROM ROUTE90;

# clean up if required
DROP VIEW ROUTE216;
DROP VIEW all_tripvibe;
DROP SOURCE tripvibe;
```
