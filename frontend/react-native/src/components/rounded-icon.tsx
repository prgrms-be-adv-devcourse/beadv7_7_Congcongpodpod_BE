import type { IconComponent, IconProps } from 'reicon-react-native/createIcon';
import { Add } from 'reicon-react-native/icons/Add';
import { AlertCircle } from 'reicon-react-native/icons/AlertCircle';
import { ArrowDown } from 'reicon-react-native/icons/ArrowDown';
import { ArrowLeft } from 'reicon-react-native/icons/ArrowLeft';
import { ArrowRight } from 'reicon-react-native/icons/ArrowRight';
import { BagCheck } from 'reicon-react-native/icons/BagCheck';
import { BagShopping } from 'reicon-react-native/icons/BagShopping';
import { Bell } from 'reicon-react-native/icons/Bell';
import { Box } from 'reicon-react-native/icons/Box';
import { Camera } from 'reicon-react-native/icons/Camera';
import { CartShopping } from 'reicon-react-native/icons/CartShopping';
import { Chat } from 'reicon-react-native/icons/Chat';
import { Check } from 'reicon-react-native/icons/Check';
import { CheckCircle } from 'reicon-react-native/icons/CheckCircle';
import { ChevronDown } from 'reicon-react-native/icons/ChevronDown';
import { ChevronLeft } from 'reicon-react-native/icons/ChevronLeft';
import { ChevronRight } from 'reicon-react-native/icons/ChevronRight';
import { ChevronUp } from 'reicon-react-native/icons/ChevronUp';
import { Clock } from 'reicon-react-native/icons/Clock';
import { CloudX } from 'reicon-react-native/icons/CloudX';
import { Coffee } from 'reicon-react-native/icons/Coffee';
import { Compass } from 'reicon-react-native/icons/Compass';
import { CreditCard } from 'reicon-react-native/icons/CreditCard';
import { DocumentText } from 'reicon-react-native/icons/DocumentText';
import { Edit } from 'reicon-react-native/icons/Edit';
import { Exit } from 'reicon-react-native/icons/Exit';
import { Flame } from 'reicon-react-native/icons/Flame';
import { FoodTray } from 'reicon-react-native/icons/FoodTray';
import { ForkKnife } from 'reicon-react-native/icons/ForkKnife';
import { Grid } from 'reicon-react-native/icons/Grid';
import { Headset } from 'reicon-react-native/icons/Headset';
import { Heart } from 'reicon-react-native/icons/Heart';
import { HelpCircle } from 'reicon-react-native/icons/HelpCircle';
import { Home } from 'reicon-react-native/icons/Home';
import { Hourglass } from 'reicon-react-native/icons/Hourglass';
import { Image } from 'reicon-react-native/icons/Image';
import { Images } from 'reicon-react-native/icons/Images';
import { InfoCircle } from 'reicon-react-native/icons/InfoCircle';
import { Leaf } from 'reicon-react-native/icons/Leaf';
import { List } from 'reicon-react-native/icons/List';
import { Location } from 'reicon-react-native/icons/Location';
import { Lock } from 'reicon-react-native/icons/Lock';
import { MapPoint } from 'reicon-react-native/icons/MapPoint';
import { Minus } from 'reicon-react-native/icons/Minus';
import { Money } from 'reicon-react-native/icons/Money';
import { Pen } from 'reicon-react-native/icons/Pen';
import { PizzaSlice } from 'reicon-react-native/icons/PizzaSlice';
import { Qr } from 'reicon-react-native/icons/Qr';
import { ReceiptText } from 'reicon-react-native/icons/ReceiptText';
import { Refresh } from 'reicon-react-native/icons/Refresh';
import { Route } from 'reicon-react-native/icons/Route';
import { Scanner } from 'reicon-react-native/icons/Scanner';
import { Search } from 'reicon-react-native/icons/Search';
import { ShieldTick } from 'reicon-react-native/icons/ShieldTick';
import { Sliders } from 'reicon-react-native/icons/Sliders';
import { Sparkles } from 'reicon-react-native/icons/Sparkles';
import { Star2 } from 'reicon-react-native/icons/Star2';
import { Store } from 'reicon-react-native/icons/Store';
import { TagPrice } from 'reicon-react-native/icons/TagPrice';
import { Trash } from 'reicon-react-native/icons/Trash';
import { TrendDown } from 'reicon-react-native/icons/TrendDown';
import { User } from 'reicon-react-native/icons/User';
import { UserMinus } from 'reicon-react-native/icons/UserMinus';
import { Wallet } from 'reicon-react-native/icons/Wallet';
import { Wineglass } from 'reicon-react-native/icons/Wineglass';
import { X } from 'reicon-react-native/icons/X';
import { XCircle } from 'reicon-react-native/icons/XCircle';

type Props = IconProps & {
  name: string;
};

const iconMap: Record<string, IconComponent> = {
  add: Add,
  'alert-circle': AlertCircle,
  'alert-circle-outline': AlertCircle,
  'arrow-down': ArrowDown,
  'arrow-left': ArrowLeft,
  'arrow-forward': ArrowRight,
  'bag-check-outline': BagCheck,
  'bag-handle-outline': BagShopping,
  cafe: Coffee,
  'camera-outline': Camera,
  'card-outline': CreditCard,
  'cart-outline': CartShopping,
  'cash-outline': Money,
  'chatbox-ellipses-outline': Chat,
  checkmark: Check,
  'checkmark-circle': CheckCircle,
  'checkmark-circle-outline': CheckCircle,
  'chevron-back': ChevronLeft,
  'chevron-down': ChevronDown,
  'chevron-forward': ChevronRight,
  'chevron-up': ChevronUp,
  close: X,
  'close-circle': XCircle,
  'close-circle-outline': XCircle,
  'cloud-offline-outline': CloudX,
  compass: Compass,
  'create-outline': Edit,
  'cube-outline': Box,
  dishes: FoodTray,
  'document-text-outline': DocumentText,
  exit: Exit,
  'exit-outline': Exit,
  'fast-food': FoodTray,
  'fast-food-outline': FoodTray,
  favorites: Heart,
  fish: ForkKnife,
  flame: Flame,
  'headset-outline': Headset,
  heart: Heart,
  'heart-outline': Heart,
  'help-circle-outline': HelpCircle,
  home: Home,
  'home-outline': Home,
  'hourglass-outline': Hourglass,
  grid: Grid,
  'grid-outline': Grid,
  image: Image,
  'image-outline': Image,
  'images-outline': Images,
  index: Home,
  'information-circle-outline': InfoCircle,
  leaf: Leaf,
  'leaf-outline': Leaf,
  list: List,
  'list-outline': List,
  locate: MapPoint,
  'locate-outline': MapPoint,
  location: Location,
  'location-outline': Location,
  'lock-closed': Lock,
  'lock-closed-outline': Lock,
  'log-out-outline': Exit,
  'megaphone-outline': Bell,
  my: User,
  navigate: Route,
  'notifications-outline': Bell,
  nutrition: FoodTray,
  'options-outline': Sliders,
  orders: ReceiptText,
  person: User,
  'person-outline': User,
  'pencil-outline': Pen,
  'person-remove-outline': UserMinus,
  pizza: PizzaSlice,
  'pricetag-outline': TagPrice,
  'qr-code-outline': Qr,
  receipt: ReceiptText,
  'receipt-outline': ReceiptText,
  refresh: Refresh,
  remove: Minus,
  restaurant: ForkKnife,
  'restaurant-outline': ForkKnife,
  'return-down-back-outline': ArrowDown,
  'scan-outline': Scanner,
  search: Search,
  settlements: Wallet,
  'shield-checkmark-outline': ShieldTick,
  sparkles: Sparkles,
  'sparkles-outline': Sparkles,
  'star-outline': Star2,
  store: Store,
  'storefront-outline': Store,
  stores: List,
  time: Clock,
  'time-outline': Clock,
  'trash-outline': Trash,
  'trending-down-outline': TrendDown,
  wallet: Wallet,
  'wallet-outline': Wallet,
  wine: Wineglass,
};

const filledNames = new Set([
  'home', 'index', 'list', 'stores', 'heart', 'favorites', 'receipt', 'orders',
  'person', 'my', 'grid', 'fast-food', 'dishes', 'wallet', 'settlements', 'store',
]);

function RoundedIconComponent({ name, size = 24, color = '#171A18', weight, ...props }: Props) {
  const Icon = iconMap[name] ?? InfoCircle;
  return <Icon {...props} color={color} size={size} weight={weight ?? (filledNames.has(name) ? 'Filled' : 'Outline')} />;
}

export const RoundedIcon = Object.assign(RoundedIconComponent, {
  glyphMap: {} as Record<string, number>,
});
