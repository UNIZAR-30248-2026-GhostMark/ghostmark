import { colors } from "./colors";

export const typography = {
  title: {
    color: colors.white,
    fontFamily: "Bangers",
    fontSize: 34,
    letterSpacing: 1,
  },
  heading: {
    color: colors.text,
    fontFamily: "ZenMaruBold",
    fontSize: 20,
  },
  body: {
    color: colors.text,
    fontFamily: "ZenMaru",
    fontSize: 16,
  },
} as const;
