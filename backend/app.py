from flask import Flask, request, jsonify
from flask_cors import CORS
from db_config import get_db_connection

app = Flask(__name__)
CORS(app)

# =================== User Signup ===================
@app.route('/api/signup', methods=['POST'])
def signup():
    data = request.json
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute("INSERT INTO users (name, email, password) VALUES (%s, %s, %s)",
                       (data['name'], data['email'], data['password']))
        conn.commit()
        return jsonify({'message': 'Signup successful'})
    except Exception as e:
        return jsonify({'message': 'Email already exists'}), 400
    finally:
        cursor.close()
        conn.close()

# =================== User Login ===================
@app.route('/api/login', methods=['POST'])
def login():
    data = request.json
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM users WHERE email = %s AND password = %s", (data['email'], data['password']))
    user = cursor.fetchone()
    cursor.close()
    conn.close()
    if user:
        return jsonify({'message': 'Login successful', 'user_id': user['id']})
    else:
        return jsonify({'message': 'Invalid credentials'}), 401

# =================== Add Pet ===================
@app.route('/api/pets', methods=['POST'])
def add_pet():
    data = request.json
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("INSERT INTO pets (name, species, breed, age, user_id) VALUES (%s, %s, %s, %s, %s)",
                   (data['name'], data['species'], data['breed'], data['age'], data['user_id']))
    conn.commit()
    cursor.close()
    conn.close()
    return jsonify({'message': 'Pet added successfully'})

# =================== Vaccination ===================
@app.route('/api/vaccination', methods=['POST'])
def add_vaccination():
    data = request.json
    conn = get_db_connection()
    cursor = conn.cursor(buffered=True)  # ✅ IMPORTANT: buffered cursor

    try:
        # Get pet by name
        cursor.execute("SELECT id FROM pets WHERE name = %s", (data['pet_name'],))
        pet = cursor.fetchone()  # ✅ Must fetch to clear results

        if not pet:
            return jsonify({'message': 'Pet not found'}), 404

        # Insert vaccination record
        cursor.execute("INSERT INTO vaccinations (pet_id, date) VALUES (%s, %s)", (pet[0], data['date']))
        conn.commit()

        return jsonify({'message': 'Vaccination booked'})
    except Exception as e:
        print("Error in /api/vaccination:", e)  # helpful debug
        return jsonify({'message': 'Error: ' + str(e)}), 500
    finally:
        cursor.close()
        conn.close()


# =================== Feeding Schedule ===================
@app.route('/api/feeding', methods=['POST'])
def add_feeding():
    data = request.json
    conn = get_db_connection()
    cursor = conn.cursor(buffered=True)  # ✅ Buffered

    try:
        cursor.execute("SELECT id FROM pets WHERE name = %s", (data['pet_name'],))
        pet = cursor.fetchone()  # ✅ Must consume SELECT result

        if not pet:
            return jsonify({'message': 'Pet not found'}), 404

        cursor.execute(
            "INSERT INTO feedings (pet_id, time, description) VALUES (%s, %s, %s)",
            (pet[0], data['time'], data['description'])
        )
        conn.commit()
        return jsonify({'message': 'Feeding schedule added'})
    except Exception as e:
        print("Error in /api/feeding:", e)
        return jsonify({'message': 'Error: ' + str(e)}), 500
    finally:
        cursor.close()
        conn.close()


# =================== Vet Visit ===================
@app.route('/api/vet-visit', methods=['POST'])
def add_vet_visit():
    data = request.json
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT id FROM pets WHERE name = %s", (data['pet_name'],))
    pet = cursor.fetchone()
    if not pet:
        return jsonify({'message': 'Pet not found'}), 404
    cursor.execute("INSERT INTO vet_visits (pet_id, date, reason) VALUES (%s, %s, %s)",
                   (pet[0], data['date'], data['reason']))
    conn.commit()
    cursor.close()
    conn.close()
    return jsonify({'message': 'Vet visit scheduled'})

if __name__ == '__main__':
    app.run(debug=True)


