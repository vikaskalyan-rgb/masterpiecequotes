-- V3: rooms, items, material specs and terms take ids 50 at a time from a plain sequence
-- (<table>_id_seq), so Hibernate can insert a whole quote in one batch instead of one round trip
-- per row.
--
-- Production's tables use serial columns (sequence + default); databases created from V1 use
-- identity columns. Both end up the same: a bigint id whose default is nextval('<table>_id_seq')
-- with an increment of 50. The previous release inserts without an id, so it draws from the same
-- sequence and keeps working after a rollback.
do $$
declare
    t   text;
    seq text;
begin
    foreach t in array array['rooms', 'items', 'material_spec_items', 'term_items'] loop
        seq := t || '_id_seq';
        if exists (select 1 from information_schema.columns
                   where table_schema = current_schema() and table_name = t and column_name = 'id' and is_identity = 'YES') then
            execute format('alter table %I alter column id drop identity', t);
            execute format('create sequence %I increment by 50 owned by %I.id', seq, t);
            execute format('alter table %I alter column id set default nextval(%L)', t, seq);
        else
            execute format('alter sequence %s increment by 50', pg_get_serial_sequence(t, 'id'));
        end if;
        -- Continue after the highest id in use (pooled-lo hands out nextval .. nextval + 49).
        execute format('select setval(%L, greatest((select coalesce(max(id), 0) from %I), 1))',
                       pg_get_serial_sequence(t, 'id'), t);
    end loop;
end $$;
