import random
from collections import Counter
import os
import json

###### 
###### 
###### ACHTUNG!!!!!!!!!!!!!!!
FOLDER_PATH = os.path.expanduser(r"~\dating-app\src\current\datingapp\app\src\main\res\raw")
###### ACHTUNG!!!!!!!!!!!!!!!
###### 
######


# Mein lielber Lionel,
# vielen Dank für deinen liebenswerten wunsch, dass ich Kommentare ein bauen muss. Extra für dich hab ich nochmal in random zeilen rein schreiben müssen was die machen, auch wenn du scheiß opfer eh versehst was sie machen. Tu nicht so als ob du so blöd bist und das nicht checkst. Du drecks nutten.
# Mit Freundlichen Grüßen
# Dein Jakob der wirklich überhaupt nicht generft ist kommentare ein bauen zu müssen
def get_rating(score):
    return score / 20


# lionel hier werden user geladen! Cool oder richtig was besonderes.......drecks arschlochhhhhhhh
def load_users():
    users = []

    for filename in os.listdir(FOLDER_PATH):
        if not filename.endswith(".json"):
            continue

        path = os.path.join(FOLDER_PATH, filename)

        with open(path, "r", encoding="utf-8") as file:
            data = json.load(file)

            try:
                user = {
                    "id": data["id"],
                    "name": data["name"],
                    "age": data["age"],
                    "gender": data["gender"],
                    "pronouns": data["pronouns"],
                    "location": data["location"],
                    "species": data["species"],
                    "fur_color": data["fur_color"],
                    "tail_type": data["tail_type"],
                    "activities": data["activities"],
                    "personality_traits": data["personality_traits"],
                    "bio": data["bio"]
                }
                users.append(user)
            except:
                continue

    return users


#perfext match average was auch immer wei lionel eine kleine nutte ist und mir extra arbeit machen will ich fick dich weg du nazi
def build_ideal_profile(liked_users):
    if not liked_users:
        return None

    ideal = {}

    keys = [
        "age", "gender", "pronouns", "location",
        "species", "fur_color", "tail_type"
    ]

    for key in keys:
        values = [u[key] for u in liked_users]
        ideal[key] = most_common(values) # du weißt gar nicht wie gerne ich dich anschreien würde dafür das du DAFÜR EINE LIBRARY IMPORTIERT HAST, ES SIND LEGIT 2 MINUTEN SELBST SCHREIBEN UND WARUM SOLLTE MAN UNNÖTIGER WEISE 30.000 LIBRARIES IMPORTIEREN (jakob ich glaube die kommentare treffen auf dich viel besser zu)

    activities = []
    traits = []

    for u in liked_users:
        activities.extend(u["activities"])
        traits.extend(u["personality_traits"])

    if activities:
        ideal["activities"] = most_common(activities)

    if traits:
        ideal["personality_traits"] = most_common(traits)

    return ideal


# JAKOB ES KANN NICHT SO SCHWER SEIN DIESE FUNCTION ZU SCHREIBEN ODER SIE ZU PROMPTEN ES IST SO EIN PAIN MIT COLLECTIONS ZU ARBEITEN MIT CHAQUOPY
def most_common(values):
    counts = {}
    for v in values:
        counts[v] = counts.get(v, 0) + 1
    max_count = -1
    max_item = None
    for k, v in counts.items():
        if v > max_count:
            max_count = v
            max_item = k
    return max_item



#SCORE 1v1 epic fight IDEALPROFIL weil du immernoch ein fuck ASS NUTTEN BIST IMMER EXTTRA ARVBEIT KANN LEGIT NICHT SEIN DU NUTTE
def score_with_ideal(user, ideal):
    score = 0

    if user["gender"] == ideal["gender"]:
        score += 5

    if user["age"] == ideal["age"]: # blud will mir erklären dass menschen so fixiert auf ein alter sind, ja ja jakob bestimmt kein sloppy code
        score += 3.5

    if user["species"] == ideal["species"]:
        score += 2

    if user["fur_color"] == ideal["fur_color"]:
        score += 2

    if user["tail_type"] == ideal["tail_type"]:
        score += 1.5

    if user["location"] == ideal["location"]:
        score += 2.5

    if user["pronouns"] == ideal["pronouns"]:
        score += 0.5

    if "activities" in ideal and ideal["activities"] in user["activities"]:
        score += 2

    if "personality_traits" in ideal and ideal["personality_traits"] in user["personality_traits"]:
        score += 2

    return score


# lionel das ist die zupa dupa tolle  app oder so lel
class DatingApp:
    def __init__(self):

        self.users = load_users()

        self.index = 0
        self.liked = []
        self.seen = []                                                                  #gesehene profile damit kein doppelt typ sii

        self.next_user = self.get_next_user()

        self.update()


    # ächsten user getten
    def get_next_user(self):

        possible = [
            u for u in self.users
            if u not in self.seen
        ]

        if not possible:
            return None

        if not self.liked:
            return random.choice(possible)

        ideal = build_ideal_profile(self.liked)

        scored = [(u, score_with_ideal(u, ideal)) for u in possible]
        scored.sort(key=lambda x: x[1], reverse=True)

        return scored[0][0]


    def update(self):

        if self.index > 0 and self.index % 5 == 0 and self.liked:
            ideal = build_ideal_profile(self.liked)

            # text = "Dein aktuelles Idealprofil:\n\n"
            # for k, v in ideal.items():
            #     text += f"{k}: {v}\n"


        if not self.next_user:
            return 0

        user = self.next_user

        if not self.liked:
            rating = "Unknown"
        else:
            ideal = build_ideal_profile(self.liked)
            score = score_with_ideal(user, ideal)
            rating = get_rating(score)

        return (rating * 100, user["id"])

#         text = f"""
# {user['name']} ({user['age']})

# Geschlecht: {user['gender']}
# Pronomen: {user['pronouns']}
# Ort: {user['location']}
# Spezies: {user['species']}
# Fellfarbe: {user['fur_color']}
# Schwanz: {user['tail_type']}

# Aktivitäten: {", ".join(user['activities'])}
# Eigenschaften: {", ".join(user['personality_traits'])}

# Bio:
# {user['bio']}

# {score_text}
# Bewertung: {rating}
# """


    def like(self):
        self.liked.append(self.next_user)
        self.seen.append(self.next_user)

        self.index += 1
        self.next_user = self.get_next_user()
        self.update()


    def dislike(self):
        self.seen.append(self.next_user)

        self.index += 1
        self.next_user = self.get_next_user()
        self.update()


    # def show_likes(self):
    #     if not self.liked:
    #         messagebox.showinfo("Likes", "Keine Likes")
    #         return

    #     names = "\n".join([u["name"] for u in self.liked])
    #     messagebox.showinfo("Likes", names)


# start
DatingApp()