import { createContext, type PropsWithChildren, useContext, useMemo, useState } from 'react';

export type StoreAvailabilityMode = 'TODAY' | 'NOW' | 'ALL';

type StoreAvailabilityContextValue = {
  availabilityMode: StoreAvailabilityMode;
  setAvailabilityMode: (value: StoreAvailabilityMode) => void;
};

const StoreAvailabilityContext = createContext<StoreAvailabilityContextValue | null>(null);

export function StoreAvailabilityProvider({ children }: PropsWithChildren) {
  const [availabilityMode, setAvailabilityMode] = useState<StoreAvailabilityMode>('NOW');
  const value = useMemo(() => ({ availabilityMode, setAvailabilityMode }), [availabilityMode]);
  return <StoreAvailabilityContext.Provider value={value}>{children}</StoreAvailabilityContext.Provider>;
}

export function useStoreAvailability() {
  const value = useContext(StoreAvailabilityContext);
  if (!value) throw new Error('useStoreAvailability must be used inside StoreAvailabilityProvider');
  return value;
}
