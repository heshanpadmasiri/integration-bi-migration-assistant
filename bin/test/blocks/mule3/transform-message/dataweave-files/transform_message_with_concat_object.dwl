%dw 1.0
%output application/json
---
concat: {aa: "a"} ++ {cc: "c"}
