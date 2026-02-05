import http from "k6/http";
import {check} from "k6";

export const options = {
    vus: 100,
    duration: "30s"
};

export default function () {
    check(http.get("http://localhost:37100/api/v2/episode-mappings"), {"status was 200": (r) => r.status === 200});
}