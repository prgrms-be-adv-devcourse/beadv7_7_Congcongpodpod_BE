import { createContext, type PropsWithChildren, useContext, useMemo, useState } from 'react';

type StoreAvailabilityContextValue = {
  onlyAvailable: boolean;
  setOnlyAvailable: (value: boolean) => void;
};

const StoreAvailabilityContext = createContext<StoreAvailabilityContextValue | null>(null);

export function StoreAvailabilityProvider({ children }: PropsWithChildren) {
  const [onlyAvailable, setOnlyAvailable] = useState(true);
  const value = useMemo(() => ({ onlyAvailable, setOnlyAvailable }), [onlyAvailable]);
  return <StoreAvailabilityContext.Provider value={value}>{children}</StoreAvailabilityContext.Provider>;
}

export function useStoreAvailability() {
  const value = useContext(StoreAvailabilityContext);
  if (!value) throw new Error('useStoreAvailability must be used inside StoreAvailabilityProvider');
  return value;
}
