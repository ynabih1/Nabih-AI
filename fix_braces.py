with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

old_code = """                    }
                }
            }
        }
    }
}

@Composable
fun MainDrawerContent("""

new_code = """                    }
                }
            }
        }
        }
    }
}

@Composable
fun MainDrawerContent("""

code = code.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

