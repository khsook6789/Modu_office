import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import AppRouter from "./routes/AppRouter";
import { ToastProvider } from "./components/Toast";
import ErrorBoundary from "./components/ErrorBoundary";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <ErrorBoundary>
        <ToastProvider>
          <AppRouter />
        </ToastProvider>
      </ErrorBoundary>
    </BrowserRouter>
  </React.StrictMode>
);
