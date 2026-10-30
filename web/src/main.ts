import "./style.css";

const root = document.createElement("div");
root.className = "placeholder";

const name = document.createElement("h1");
name.textContent = "w.tv";

const version = document.createElement("p");
version.textContent = __APP_VERSION__;

root.append(name, version);
document.body.append(root);
