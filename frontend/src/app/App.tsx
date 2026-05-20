import { AppProviders } from "./providers";
import { AppRoutes } from "../routes/AppRoutes";

function App() {
    return (
        <AppProviders>
            <AppRoutes />
        </AppProviders>
    );
}

export default App;
