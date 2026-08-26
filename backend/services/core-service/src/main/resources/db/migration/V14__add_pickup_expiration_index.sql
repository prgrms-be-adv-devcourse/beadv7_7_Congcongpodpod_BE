CREATE INDEX idx_orders_pickup_expiration
    ON public.orders USING btree (pickup_deadline, id)
    WHERE is_deleted = false AND status = 'PICKUP_READY';
